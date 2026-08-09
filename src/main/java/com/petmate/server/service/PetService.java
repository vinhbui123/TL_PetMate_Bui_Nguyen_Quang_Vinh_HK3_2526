package com.petmate.server.service;

import com.petmate.server.dto.PetRequestDto;
import com.petmate.server.dto.RedListCheckResult;
import com.petmate.server.entity.Pet;
import com.petmate.server.entity.SavedPet;
import com.petmate.server.entity.User;
import com.petmate.server.enums.AdStatus;
import com.petmate.server.enums.ListingType;
import com.petmate.server.enums.RoleType;
import com.petmate.server.repository.PetRepository;
import com.petmate.server.repository.SavedPetRepository;
import com.petmate.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import com.petmate.server.entity.OrganizationProfile;
import com.petmate.server.repository.OrganizationProfileRepository;
import com.petmate.server.repository.OrganizationMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PetService {

    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final FirebaseService firebaseService;
    private final SavedPetRepository savedPetRepository;
    private final UserService userService; // To reuse findCurrentUser
    private final OrganizationProfileRepository orgRepository;
    private final OrganizationMemberRepository memberRepository;
    private final RedListService redListService;

    public List<Pet> getAllPets(String category) {
        if (category != null && !category.isEmpty()) {
            return petRepository.findByCategoryAndStatusOrderByLikeCountDesc(category, AdStatus.AVAILABLE);
        }
        return petRepository.findByStatusOrderByLikeCountDesc(AdStatus.AVAILABLE);
    }

    public Pet getPetById(Long id) {
        return petRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thú cưng"));
    }

    public List<Pet> getPetsByUserId(Long userId) {
        return petRepository.findByUserId(userId);
    }

    public List<Pet> getMyPets(Jwt jwt) {
        String uid = jwt.getSubject();
        return petRepository.findByUser_ProviderId(uid);
    }

    public List<Pet> getPetsByOrganizationId(Long orgId, Jwt jwt) {
        // Option to verify if user is member of org could be added here, but dashboard expects org pets.
        return petRepository.findByOrganizationId(orgId);
    }

    public List<Pet> getSavedPets(Jwt jwt) {
        String uid = jwt.getSubject();
        return savedPetRepository.findByUser_ProviderIdOrderByCreatedAtDesc(uid)
                .stream()
                .map(SavedPet::getPet)
                .toList();
    }

    public Pet createPet(Jwt jwt, PetRequestDto dto) {
        ListingType listingType = Optional.ofNullable(dto.getListingType()).orElse(ListingType.SALE);

        if (listingType == ListingType.SALE && (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bắt buộc phải nhập giá cho bài đăng bán");
        }

        User owner = userService.getCurrentUserAndUpdateActivity(jwt);
        OrganizationProfile org = null;
        if (dto.getOrganizationId() != null) {
            org = orgRepository.findById(dto.getOrganizationId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tổ chức"));
            
            boolean isMember = memberRepository.existsByOrganizationIdAndUserId(org.getId(), owner.getId());
            boolean isOwner = org.getUser().getId().equals(owner.getId());
            if (!isMember && !isOwner) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền đăng bài thay mặt cho tổ chức này");
            }
        }

        Pet pet = Pet.builder()
                .name(dto.getName())
                .breed(dto.getBreed())
                .listingType(listingType)
                .ageMonths(dto.getAge() != null && !dto.getAge().isEmpty() ? Integer.parseInt(dto.getAge()) : null)
                .weight(dto.getWeight() != null && !dto.getWeight().isEmpty() ? Double.parseDouble(dto.getWeight()) : null)
                .gender(dto.getGender())
                .price(dto.getPrice())
                .isVaccinated(Optional.ofNullable(dto.getIsVaccinated()).orElse(false))
                .isNeutered(Optional.ofNullable(dto.getIsNeutered()).orElse(false))
                .description(dto.getDescription())
                .category(dto.getCategory())
                .status(AdStatus.PENDING)
                .address(dto.getAddress() != null ? dto.getAddress() : owner.getAddress())
                .latitude(dto.getLatitude() != null ? dto.getLatitude() : owner.getLatitude())
                .longitude(dto.getLongitude() != null ? dto.getLongitude() : owner.getLongitude())
                .user(owner)
                .organization(org)
                .build();

        RedListCheckResult redListResult = redListService.checkPet(dto);
        if (redListResult.isMatched()) {
            if (redListResult.getSpecies().getProtectionLevel() == com.petmate.server.enums.ProtectionLevel.PROHIBITED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Loại thú cưng này thuộc Danh sách đỏ cấm giao dịch ("
                                + redListResult.getSpecies().getBreedKeyword()
                                + "). Không thể đăng tin.");
            }
            pet.setStatus(AdStatus.REQUIRES_REVIEW);
            pet.setRedListNote("Phát hiện từ khóa Danh sách đỏ: "
                    + redListResult.getMatchedKeyword()
                    + " (loại khớp: " + redListResult.getMatchType() + ")");
        }

        return petRepository.save(pet);
    }

    public List<Pet> getPendingPets(Jwt jwt) {
        User user = userService.getCurrentUserOrThrow(jwt);
        if (user.getRole() != RoleType.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Từ chối truy cập");
        }
        return petRepository.findByStatusOrderByLikeCountDesc(AdStatus.PENDING);
    }

    public List<Pet> getPendingRedListPets(Jwt jwt) {
        User user = userService.getCurrentUserOrThrow(jwt);
        if (user.getRole() != RoleType.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Từ chối truy cập");
        }
        return petRepository.findByStatusOrderByLikeCountDesc(AdStatus.REQUIRES_REVIEW);
    }

    public List<Pet> getAdminAllPets(Jwt jwt) {
        User user = userService.getCurrentUserOrThrow(jwt);
        if (user.getRole() != RoleType.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Từ chối truy cập");
        }
        return petRepository.findAll();
    }

    public Pet updatePetStatus(Jwt jwt, Long id, AdStatus status) {
        User currentUser = userService.getCurrentUserOrThrow(jwt);
        String uid = jwt.getSubject();

        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thú cưng"));

        boolean isAdmin = currentUser.getRole() == RoleType.ADMIN;
        boolean isOwner = pet.getUser() != null && uid.equals(pet.getUser().getProviderId());
        boolean isOrgMember = pet.getOrganization() != null && memberRepository.existsByOrganizationIdAndUserId(pet.getOrganization().getId(), currentUser.getId());
        boolean isOrgOwner = pet.getOrganization() != null && pet.getOrganization().getUser().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner && !isOrgMember && !isOrgOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Từ chối truy cập");
        }

        pet.setStatus(status);
        petRepository.save(pet);

        Optional.of(pet)
                .map(Pet::getUser)
                .filter(u -> isAdmin && !isOwner)
                .ifPresent(u -> {
                    String title = "Cập nhật Tin đăng";
                    String body = status == AdStatus.AVAILABLE 
                        ? "Tin đăng thú cưng '" + pet.getName() + "' của bạn đã được duyệt!" 
                        : (status == AdStatus.REJECTED ? "Tin đăng thú cưng '" + pet.getName() + "' của bạn đã bị từ chối." : "Tin đăng của bạn đã được cập nhật.");
                    firebaseService.sendNotification(u.getId(), title, body, null);
                });

        return pet;
    }

    public Pet updatePet(Jwt jwt, Long id, PetRequestDto dto) {
        String uid = jwt.getSubject();
        
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thú cưng"));
        
        User currentUser = userService.getCurrentUserOrThrow(jwt);
        boolean isOwner = pet.getUser() != null && uid.equals(pet.getUser().getProviderId());
        boolean isOrgMember = pet.getOrganization() != null && memberRepository.existsByOrganizationIdAndUserId(pet.getOrganization().getId(), currentUser.getId());
        boolean isOrgOwner = pet.getOrganization() != null && pet.getOrganization().getUser().getId().equals(currentUser.getId());

        if (!isOwner && !isOrgMember && !isOrgOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Từ chối truy cập");
        }

        Optional.ofNullable(dto.getName()).ifPresent(pet::setName);
        Optional.ofNullable(dto.getBreed()).ifPresent(pet::setBreed);
        Optional.ofNullable(dto.getListingType()).ifPresent(pet::setListingType);
        Optional.ofNullable(dto.getAge()).filter(s -> !s.isEmpty()).ifPresent(a -> pet.setAgeMonths(Integer.parseInt(a)));
        Optional.ofNullable(dto.getWeight()).filter(s -> !s.isEmpty()).ifPresent(w -> pet.setWeight(Double.parseDouble(w)));
        Optional.ofNullable(dto.getGender()).ifPresent(pet::setGender);
        Optional.ofNullable(dto.getPrice()).ifPresent(pet::setPrice);
        Optional.ofNullable(dto.getIsVaccinated()).ifPresent(pet::setIsVaccinated);
        Optional.ofNullable(dto.getIsNeutered()).ifPresent(pet::setIsNeutered);
        Optional.ofNullable(dto.getDescription()).ifPresent(pet::setDescription);
        Optional.ofNullable(dto.getCategory()).ifPresent(pet::setCategory);
        Optional.ofNullable(dto.getStatus()).ifPresent(pet::setStatus);
        Optional.ofNullable(dto.getAddress()).ifPresent(pet::setAddress);
        Optional.ofNullable(dto.getLatitude()).ifPresent(pet::setLatitude);
        Optional.ofNullable(dto.getLongitude()).ifPresent(pet::setLongitude);

        if (dto.getOrganizationId() != null) {
            com.petmate.server.entity.OrganizationProfile org = orgRepository.findById(dto.getOrganizationId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tổ chức"));
            boolean isMember = memberRepository.existsByOrganizationIdAndUserId(org.getId(), currentUser.getId());
            boolean isOrgOwnerCheck = org.getUser().getId().equals(currentUser.getId());
            if (!isMember && !isOrgOwnerCheck) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền đăng bài thay mặt cho tổ chức này");
            }
            pet.setOrganization(org);
        } else {
            pet.setOrganization(null);
        }

        return petRepository.save(pet);
    }

    public void deletePet(Jwt jwt, Long id) {
        User currentUser = userService.getCurrentUserOrThrow(jwt);
        String uid = jwt.getSubject();

        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thú cưng"));
        
        boolean isOwner = pet.getUser() != null && uid.equals(pet.getUser().getProviderId());
        boolean isOrgMember = pet.getOrganization() != null && memberRepository.existsByOrganizationIdAndUserId(pet.getOrganization().getId(), currentUser.getId());
        boolean isOrgOwner = pet.getOrganization() != null && pet.getOrganization().getUser().getId().equals(currentUser.getId());

        if (!isOwner && !isOrgMember && !isOrgOwner && currentUser.getRole() != RoleType.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Từ chối truy cập");
        }

        petRepository.deleteById(id);
    }

    public Pet uploadPetImage(Jwt jwt, Long id, MultipartFile file) {
        String uid = jwt.getSubject();
        User currentUser = userService.getCurrentUserAndUpdateActivity(jwt);

        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thú cưng"));

        if (pet.getUser() == null || !uid.equals(pet.getUser().getProviderId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Từ chối truy cập");
        }

        try {
            String imageUrl = cloudinaryService.uploadImage(file);
            pet.setImageUrl(imageUrl);
            return petRepository.save(pet);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi khi tải ảnh lên", e);
        }
    }
}

