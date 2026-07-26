package com.petmate.server.config;

import com.petmate.server.entity.Pet;
import com.petmate.server.repository.PetRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;

@Component
public class ExcelDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ExcelDataSeeder.class);
    private final PetRepository petRepository;

    public ExcelDataSeeder(PetRepository petRepository) {
        this.petRepository = petRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (petRepository.count() > 0) {
            log.info("Báº£ng pets Ä‘Ã£ cÃ³ dá»¯ liá»‡u. Bá» qua quÃ¡ trÃ¬nh import Excel.");
            return;
        }

        File excelFile = new File("d:/petAppServer/pet data.xlsx");
        if (!excelFile.exists()) {
            log.warn("KhÃ´ng tÃ¬m tháº¥y file pet data.xlsx táº¡i thÆ° má»¥c gá»‘c. Bá» qua import.");
            return;
        }

        log.info("Báº¯t Ä‘áº§u import dá»¯ liá»‡u tá»« file Excel...");
        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0); // Láº¥y sheet Ä‘áº§u tiÃªn
            boolean isFirstRow = true;

            for (Row row : sheet) {
                if (isFirstRow) {
                    isFirstRow = false; // Bá» qua dÃ²ng header
                    continue;
                }

                // Cá»™t A: TiÃªu Äá»
                String name = getCellValue(row.getCell(0));
                // Cá»™t B: GiÃ¡
                String priceStr = getCellValue(row.getCell(1));
                // Cá»™t C: MÃ´ Táº£
                String description = getCellValue(row.getCell(2));
                // Cá»™t D: Link áº¢nh
                String imageUrl = getCellValue(row.getCell(3));

                if (name == null || name.trim().isEmpty()) {
                    continue;
                }

                BigDecimal price = null;
                if (priceStr != null && !priceStr.trim().isEmpty()) {
                    try {
                        price = new BigDecimal(priceStr.trim().replaceAll("[^0-9.]", ""));
                    } catch (Exception ignored) {}
                }

                // PhÃ¢n loáº¡i dá»±a trÃªn tÃªn + mÃ´ táº£
                String category = detectCategory(name, description);

                Pet pet = Pet.builder()
                        .name(name)
                        .price(price)
                        .description(description)
                        .imageUrl(imageUrl)
                        .category(category)
                        .breed("KhÃ´ng xÃ¡c Ä‘á»‹nh")
                        .ageMonths(null)
                        .build();

                petRepository.save(pet);
            }
            log.info("Import dá»¯ liá»‡u Excel thÃ nh cÃ´ng!");

        } catch (Exception e) {
            log.error("Lá»—i khi import file Excel: ", e);
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    private String detectCategory(String name, String description) {
        String text = (name + " " + (description != null ? description : "")).toLowerCase();

        // Tá»« khÃ³a CHÃ“
        String[] dogKeywords = {
                "chÃ³", "cÃºn", "cáº©u", "puppy", "dog",
                "poodle", "corgi", "husky", "golden", "retriever",
                "phá»‘c", "pomeranian", "chihuahua", "beagle",
                "shiba", "alaska", "bull", "pitbull", "dachshund",
                "phÃº quá»‘c", "becgie", "becgiÃª", "rottweiler",
                "labrador", "samoyed", "báº¯c kinh", "nháº­t", "pug",
                "bichon", "border collie", "chÄƒn cá»«u"
        };

        // Tá»« khÃ³a MÃˆO
        String[] catKeywords = {
                "mÃ¨o", "cat", "kitten", "kitty",
                "aln", "anh lÃ´ng ngáº¯n", "ba tÆ°", "persian",
                "scottish", "munchkin", "ragdoll", "bengal",
                "sphynx", "siamese", "maine coon", "british",
                "tai cá»¥p", "lÃ´ng dÃ i", "lÃ´ng ngáº¯n", "tabby"
        };

        // Tá»« khÃ³a CHIM Cáº¢NH
        String[] birdKeywords = {
                "chim", "váº¹t", "yáº¿n phá»¥ng", "chÃ o mÃ o", "sÃ¡o",
                "parrot", "bird", "bá»“ cÃ¢u", "cu gÃ¡y",
                "kÃ©t", "cockatiel", "canary", "hoÃ ng yáº¿n",
                "Ä‘áº¡i bÃ ng", "cÃº", "chÃ­ch chÃ²e", "sáº»"
        };

        // Tá»« khÃ³a CÃ Cáº¢NH
        String[] fishKeywords = {
                "cÃ¡", "fish", "bá»ƒ cÃ¡", "koi", "betta",
                "cÃ¡ vÃ ng", "cÃ¡ chÃ©p", "cÃ¡ rá»“ng", "cÃ¡ báº£y mÃ u",
                "cÃ¡ dÄ©a", "cÃ¡ neon", "cÃ¡ guppy", "há»“ cÃ¡"
        };

        // Tá»« khÃ³a HAMSTER / Gáº·m nháº¥m nhá»
        String[] hamsterKeywords = {
                "hamster", "chuá»™t", "chinchilla", "guinea pig",
                "sÃ³c", "nhÃ­m"
        };

        // Tá»« khÃ³a THá»Ž
        String[] rabbitKeywords = {
                "thá»", "rabbit", "bunny"
        };

        // Tá»« khÃ³a GIA Cáº¦M
        String[] poultryKeywords = {
                "gÃ ", "vá»‹t", "ngan", "ngá»—ng", "chim cÃºt",
                "gÃ  kiá»ƒng", "gÃ  tre", "gÃ  chá»i"
        };

        // Tá»« khÃ³a KHÃC (bÃ² sÃ¡t, etc.)
        String[] otherKeywords = {
                "rÃ¹a", "turtle", "ráº¯n", "snake", "rá»“ng", "gecko",
                "bÃ² sÃ¡t", "táº¯c kÃ¨", "ká»³ nhÃ´ng", "iguana"
        };

        for (String kw : dogKeywords) {
            if (text.contains(kw)) return "DOGS";
        }
        for (String kw : catKeywords) {
            if (text.contains(kw)) return "CATS";
        }
        // Gia cáº§m kiá»ƒm tra TRÆ¯á»šC chim cáº£nh vÃ¬ "gÃ " náº¿u náº±m trong birdKeywords sáº½ sai
        for (String kw : poultryKeywords) {
            if (text.contains(kw)) return "POULTRY";
        }
        for (String kw : birdKeywords) {
            if (text.contains(kw)) return "BIRDS";
        }
        for (String kw : fishKeywords) {
            if (text.contains(kw)) return "FISH";
        }
        for (String kw : hamsterKeywords) {
            if (text.contains(kw)) return "HAMSTERS";
        }
        for (String kw : rabbitKeywords) {
            if (text.contains(kw)) return "RABBITS";
        }
        for (String kw : otherKeywords) {
            if (text.contains(kw)) return "OTHER";
        }
        return "OTHER"; // Máº·c Ä‘á»‹nh vá» OTHER náº¿u khÃ´ng nháº­n diá»‡n Ä‘Æ°á»£c
    }
}
