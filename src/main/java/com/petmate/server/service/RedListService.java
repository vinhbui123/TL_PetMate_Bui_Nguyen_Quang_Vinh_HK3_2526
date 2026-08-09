package com.petmate.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petmate.server.dto.PetRequestDto;
import com.petmate.server.dto.RedListCheckResult;
import com.petmate.server.dto.RedListSpeciesDto;
import com.petmate.server.entity.RedListSpecies;
import com.petmate.server.enums.AdStatus;
import com.petmate.server.enums.ProtectionLevel;
import com.petmate.server.repository.PetRepository;
import com.petmate.server.repository.RedListSpeciesRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedListService {

    private final RedListSpeciesRepository redListRepository;
    private final PetRepository petRepository;
    private final ObjectMapper objectMapper;

    private List<RedListSpecies> cachedSpecies = new ArrayList<>();

    @PostConstruct
    public void loadCache() {
        try {
            cachedSpecies = redListRepository.findAll();
            log.info("Loaded {} red-list species into cache", cachedSpecies.size());
        } catch (Exception e) {
            log.error("Failed to load red-list species cache", e);
            cachedSpecies = new ArrayList<>();
        }
    }

    private void refreshCache() {
        loadCache();
    }

    public List<RedListSpecies> getAllSpecies() {
        if (cachedSpecies.isEmpty()) {
            return redListRepository.findAll();
        }
        return cachedSpecies;
    }

    public RedListSpecies getSpeciesById(Long id) {
        return redListRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loài trong Danh sách đỏ"));
    }

    @Transactional
    public RedListSpecies addSpecies(RedListSpeciesDto dto) {
        if (redListRepository.existsByCategoryAndBreedKeywordIgnoreCase(
                dto.getCategory(), dto.getBreedKeyword())) {
            throw new RuntimeException("Loài này đã tồn tại trong Danh sách đỏ");
        }

        RedListSpecies species = RedListSpecies.builder()
                .category(dto.getCategory())
                .breedKeyword(dto.getBreedKeyword().trim())
                .synonyms(normalizeSynonyms(dto.getSynonyms()))
                .protectionLevel(dto.getProtectionLevel() != null ? dto.getProtectionLevel() : ProtectionLevel.RESTRICTED)
                .description(dto.getDescription())
                .build();

        RedListSpecies saved = redListRepository.save(species);
        refreshCache();
        return saved;
    }

    @Transactional
    public RedListSpecies updateSpecies(Long id, RedListSpeciesDto dto) {
        RedListSpecies species = getSpeciesById(id);
        Optional.ofNullable(dto.getCategory()).ifPresent(species::setCategory);
        Optional.ofNullable(dto.getBreedKeyword()).ifPresent(v -> species.setBreedKeyword(v.trim()));
        Optional.ofNullable(dto.getSynonyms()).ifPresent(v -> species.setSynonyms(normalizeSynonyms(v)));
        Optional.ofNullable(dto.getProtectionLevel()).ifPresent(species::setProtectionLevel);
        Optional.ofNullable(dto.getDescription()).ifPresent(species::setDescription);

        RedListSpecies saved = redListRepository.save(species);
        refreshCache();
        return saved;
    }

    @Transactional
    public void deleteSpecies(Long id) {
        redListRepository.deleteById(id);
        refreshCache();
    }

    private String normalizeSynonyms(String synonyms) {
        if (synonyms == null || synonyms.trim().isEmpty()) {
            return null;
        }
        try {
            List<String> list = Arrays.stream(synonyms.split("[,;]"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return synonyms;
        }
    }

    private List<String> parseSynonyms(String synonymsJson) {
        if (synonymsJson == null || synonymsJson.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            if (synonymsJson.trim().startsWith("[")) {
                return objectMapper.readValue(synonymsJson, new TypeReference<List<String>>() {});
            }
            return Arrays.stream(synonymsJson.split("[,;]"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public RedListCheckResult checkPet(PetRequestDto dto) {
        String userText = Stream.of(dto.getName(), dto.getBreed(), dto.getDescription())
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));

        for (RedListSpecies species : getAllSpecies()) {
            List<String> keywords = new ArrayList<>();
            if (species.getBreedKeyword() != null) {
                Arrays.stream(species.getBreedKeyword().split(","))
                      .map(String::trim)
                      .filter(s -> !s.isEmpty())
                      .forEach(keywords::add);
            }
            keywords.addAll(parseSynonyms(species.getSynonyms()));

            for (String kw : keywords) {
                if (kw == null || kw.trim().isEmpty()) continue;

                if (containsWholeWord(userText, kw)) {
                    return buildResult(species, kw, "EXACT");
                }

                if (fuzzyMatchWord(userText, kw, fuzzyThreshold(kw))) {
                    return buildResult(species, kw, "FUZZY");
                }
            }
        }

        return RedListCheckResult.noMatch();
    }

    private RedListCheckResult buildResult(RedListSpecies species, String keyword, String matchType) {
        return RedListCheckResult.builder()
                .matched(true)
                .species(species)
                .matchedKeyword(keyword)
                .matchType(matchType)
                .build();
    }

    private String stripDiacritics(String s) {
        String normalized = Normalizer.normalize(s, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
    }

    private boolean containsWholeWord(String text, String keyword) {
        if (text == null || keyword == null || keyword.trim().isEmpty()) {
            return false;
        }
        String normalizedText = " " + stripDiacritics(text).toLowerCase().replaceAll("[\\s,\\.\\-_;:!?()\\[\\]\"']+", " ").trim() + " ";
        String normalizedKeyword = " " + stripDiacritics(keyword).toLowerCase().replaceAll("[\\s,\\.\\-_;:!?()\\[\\]\"']+", " ").trim() + " ";

        return normalizedText.contains(normalizedKeyword);
    }

    private boolean fuzzyMatchWord(String text, String keyword, int maxDistance) {
        if (text == null || keyword == null || keyword.trim().isEmpty() || maxDistance <= 0) {
            return false;
        }
        String cleanKeyword = stripDiacritics(keyword).toLowerCase().replaceAll("[\\s,\\.\\-_;:!?()\\[\\]\"']+", " ").trim();
        String[] textWords = stripDiacritics(text).toLowerCase()
                .replaceAll("[\\s,\\.\\-_;:!?()\\[\\]\"']+", " ").trim().split(" ");
        String[] keywordWords = cleanKeyword.split(" ");

        if (keywordWords.length == 0 || textWords.length == 0 || keywordWords[0].isEmpty()) {
            return false;
        }

        for (int i = 0; i <= textWords.length - keywordWords.length; i++) {
            int totalDistance = 0;
            for (int j = 0; j < keywordWords.length; j++) {
                totalDistance += levenshtein(textWords[i+j], keywordWords[j]);
            }
            if (totalDistance <= maxDistance) {
                return true;
            }
        }
        return false;
    }

    private int fuzzyThreshold(String keyword) {
        int len = stripDiacritics(keyword).length();
        if (len <= 3) return 0;
        if (len <= 5) return 1;
        return 2;
    }

    private int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }

    public List<com.petmate.server.entity.Pet> getPendingRedListPets() {
        return petRepository.findByStatusOrderByLikeCountDesc(AdStatus.REQUIRES_REVIEW);
    }
}
