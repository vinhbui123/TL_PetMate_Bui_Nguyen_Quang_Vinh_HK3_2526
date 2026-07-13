package com.petmate.server.config;

import com.petmate.server.entity.Pet;
import com.petmate.server.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExcelDataSeeder implements CommandLineRunner {

    private final PetRepository petRepository;

    @Override
    public void run(String... args) throws Exception {
        if (petRepository.count() > 0) {
            log.info("Bảng pets đã có dữ liệu. Bỏ qua quá trình import Excel.");
            return;
        }

        File excelFile = new File("d:/petAppServer/pet data.xlsx");
        if (!excelFile.exists()) {
            log.warn("Không tìm thấy file pet data.xlsx tại thư mục gốc. Bỏ qua import.");
            return;
        }

        log.info("Bắt đầu import dữ liệu từ file Excel...");
        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0); // Lấy sheet đầu tiên
            boolean isFirstRow = true;

            for (Row row : sheet) {
                if (isFirstRow) {
                    isFirstRow = false; // Bỏ qua dòng header
                    continue;
                }

                // Cột A: Tiêu Đề
                String name = getCellValue(row.getCell(0));
                // Cột B: Giá
                String price = getCellValue(row.getCell(1));
                // Cột C: Mô Tả
                String description = getCellValue(row.getCell(2));
                // Cột D: Link Ảnh
                String imageUrl = getCellValue(row.getCell(3));

                if (name == null || name.trim().isEmpty()) {
                    continue;
                }

                // Phân loại dựa trên tên + mô tả
                String category = detectCategory(name, description);

                Pet pet = Pet.builder()
                        .name(name)
                        .price(price)
                        .description(description)
                        .imageUrl(imageUrl)
                        .category(category)
                        .breed("Không xác định")
                        .age("Không xác định")
                        .distance("1.5km")
                        .build();

                petRepository.save(pet);
            }
            log.info("Import dữ liệu Excel thành công!");

        } catch (Exception e) {
            log.error("Lỗi khi import file Excel: ", e);
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

        // Từ khóa CHÓ
        String[] dogKeywords = {
                "chó", "cún", "cẩu", "puppy", "dog",
                "poodle", "corgi", "husky", "golden", "retriever",
                "phốc", "pomeranian", "chihuahua", "beagle",
                "shiba", "alaska", "bull", "pitbull", "dachshund",
                "phú quốc", "becgie", "becgiê", "rottweiler",
                "labrador", "samoyed", "bắc kinh", "nhật", "pug",
                "bichon", "border collie", "chăn cừu"
        };

        // Từ khóa MÈO
        String[] catKeywords = {
                "mèo", "cat", "kitten", "kitty",
                "aln", "anh lông ngắn", "ba tư", "persian",
                "scottish", "munchkin", "ragdoll", "bengal",
                "sphynx", "siamese", "maine coon", "british",
                "tai cụp", "lông dài", "lông ngắn", "tabby"
        };

        // Từ khóa CHIM
        String[] birdKeywords = {
                "chim", "vẹt", "yến", "chào mào", "sáo",
                "parrot", "bird", "bồ câu", "cu gáy",
                "két", "cockatiel", "canary", "hoàng yến",
                "gà", "đại bàng", "cú", "chích chòe", "sẻ"
        };

        // Từ khóa THÚ CƯNG KHÁC (thỏ, chuột hamster, bò sát, cá...)
        String[] otherKeywords = {
                "thỏ", "rabbit", "bunny", "hamster", "chuột",
                "rùa", "turtle", "rắn", "snake", "rồng", "gecko",
                "chinchilla", "guinea pig", "sóc", "nhím",
                "cá", "fish", "bể cá", "lồng"
        };

        for (String kw : dogKeywords) {
            if (text.contains(kw)) return "DOGS";
        }
        for (String kw : catKeywords) {
            if (text.contains(kw)) return "CATS";
        }
        for (String kw : birdKeywords) {
            if (text.contains(kw)) return "PARROT";
        }
        for (String kw : otherKeywords) {
            if (text.contains(kw)) return "RABBIT";
        }
        return "DOGS"; // Mặc định về DOGS nếu không nhận diện được
    }
}
