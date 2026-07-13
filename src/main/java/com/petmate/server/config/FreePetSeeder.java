package com.petmate.server.config;

import com.petmate.server.entity.Pet;
import com.petmate.server.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FreePetSeeder implements CommandLineRunner {

    private final PetRepository petRepository;

    @Override
    public void run(String... args) throws Exception {
        boolean isExist = petRepository.findAll().stream()
                .anyMatch(p -> p.getName() != null && p.getName().contains("Mực"));
        if (isExist) {
            log.info("Dữ liệu thú cưng miễn phí đã tồn tại. Bỏ qua.");
            return;
        }

        log.info("Bắt đầu thêm 10 thú cưng nhận nuôi miễn phí...");

        List<Pet> freePets = List.of(
                Pet.builder()
                        .name("Mực")
                        .breed("Chó Ta (Chó Cỏ)")
                        .age("3 tháng")
                        .category("DOGS")
                        .description("Bé Mực cực kỳ ngoan và quấn chủ, đã được tẩy giun. Do nhà mình sắp chuyển trọ không nuôi được nên muốn tìm chủ yêu thương bé.")
                        .distance("1.2km")
                        .price("Miễn phí")
                        .imageUrl("https://images.unsplash.com/photo-1543466835-00a7907e9de1?auto=format&fit=crop&q=80&w=500")
                        .build(),
                Pet.builder()
                        .name("Mimi")
                        .breed("Mèo Mướp")
                        .age("1 tuổi")
                        .category("CATS")
                        .description("Mimi rất hiền, chỉ thích nằm ườn tắm nắng và kêu rù rù. Bé đã triệt sản và tiêm phòng đầy đủ. Tặng kèm khay vệ sinh.")
                        .distance("3.5km")
                        .price("0 đ")
                        .imageUrl("https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&q=80&w=500")
                        .build(),
                Pet.builder()
                        .name("Bông")
                        .breed("Poodle lai Nhật")
                        .age("2 tuổi")
                        .category("DOGS")
                        .description("Bông lông trắng muốt, thích ăn xúc xích và hay nịnh. Bé hay sủa người lạ nhưng rất hiền với trẻ em.")
                        .distance("5km")
                        .price("Miễn phí")
                        .imageUrl("https://images.unsplash.com/photo-1593134257782-e89567b7718a?auto=format&fit=crop&q=80&w=500")
                        .build(),
                Pet.builder()
                        .name("Nala")
                        .breed("Mèo Tam Thể")
                        .age("4 tháng")
                        .category("CATS")
                        .description("Bé Nala được cứu hộ từ một công trường. Hiện tại bé đã khỏe mạnh, ăn hạt tốt và rất thích chơi cần câu mèo.")
                        .distance("2.1km")
                        .price(null) // null cũng được xem là miễn phí
                        .imageUrl("https://images.unsplash.com/photo-1573865526739-10659fec78a5?auto=format&fit=crop&q=80&w=500")
                        .build(),
                Pet.builder()
                        .name("Cà Rốt")
                        .breed("Thỏ Kiểng Mini")
                        .age("6 tháng")
                        .category("RABBIT")
                        .description("Mình đi làm cả ngày không có thời gian chăm sóc nên muốn nhượng lại bé thỏ Cà Rốt. Bé ăn cỏ khô và uống nước bình.")
                        .distance("10km")
                        .price("0")
                        .imageUrl("https://images.unsplash.com/photo-1585110396000-c9fd4e4e5030?auto=format&fit=crop&q=80&w=500")
                        .build(),
                Pet.builder()
                        .name("Ngáo")
                        .breed("Chó cỏ lai Husky")
                        .age("8 tháng")
                        .category("DOGS")
                        .description("Đúng như cái tên, bé rất tăng động và cần không gian rộng để chạy nhảy. Bạn nào có sân vườn rộng hãy nhận bé nhé.")
                        .distance("7.5km")
                        .price("Miễn phí")
                        .imageUrl("https://images.unsplash.com/photo-1605568420105-440fae0ed138?auto=format&fit=crop&q=80&w=500")
                        .build(),
                Pet.builder()
                        .name("Luna")
                        .breed("Mèo Đen")
                        .age("1.5 tuổi")
                        .category("CATS")
                        .description("Luna mang bộ lông đen tuyền cực ngầu. Bé có tính cách độc lập nhưng tối ngủ rất thích rúc vào nách chủ.")
                        .distance("4.2km")
                        .price("")
                        .imageUrl("https://images.unsplash.com/photo-1503431128871-161c60c60efa?auto=format&fit=crop&q=80&w=500")
                        .build(),
                Pet.builder()
                        .name("Rio")
                        .breed("Vẹt Yến Phụng")
                        .age("Không xác định")
                        .category("PARROT")
                        .description("Rio bay lạc vào ban công nhà mình, mình đã đăng tìm chủ 1 tháng không thấy ai nhận nên giờ muốn tìm người có kinh nghiệm nuôi chim để tặng lại.")
                        .distance("0.5km")
                        .price("Miễn phí")
                        .imageUrl("https://images.unsplash.com/photo-1552728089-57168a145833?auto=format&fit=crop&q=80&w=500")
                        .build(),
                Pet.builder()
                        .name("Vện")
                        .breed("Chó Phú Quốc lai")
                        .age("2.5 tuổi")
                        .category("DOGS")
                        .description("Bé có xoáy lưng rất đẹp, bắt chuột giỏi và giữ nhà cực kỳ tốt. Phù hợp cho ai cần chó giữ rẫy hoặc xưởng.")
                        .distance("12km")
                        .price("0 đ")
                        .imageUrl("https://images.unsplash.com/photo-1534361960057-19889db9621e?auto=format&fit=crop&q=80&w=500")
                        .build(),
                Pet.builder()
                        .name("Leo")
                        .breed("Mèo ALN lai")
                        .age("5 tháng")
                        .category("CATS")
                        .description("Leo form mặt khá to, lông màu xám xanh. Cực kỳ ham ăn và hay ngủ nướng. Cần tìm sen kiên nhẫn dọn wc cho hoàng thượng.")
                        .distance("8km")
                        .price("Miễn phí")
                        .imageUrl("https://images.unsplash.com/photo-1513245543132-31f507417b26?auto=format&fit=crop&q=80&w=500")
                        .build()
        );

        petRepository.saveAll(freePets);
        log.info("Đã thêm thành công 10 thú cưng miễn phí vào DB.");
    }
}
