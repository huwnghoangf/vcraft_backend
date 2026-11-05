package com.vcraft;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/**
 * Kiểm tra kiến trúc 3 tầng: API -> Nghiệp vụ (domain) -> Truy cập dữ liệu (infrastructure).
 * Đảm bảo tầng nghiệp vụ hoàn toàn độc lập với framework web và database.
 */
class ArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.vcraft");

    @Test
    void domainKhongDungFramework() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "jakarta.servlet..",
                        "org.hibernate..")
                .because("tầng nghiệp vụ chỉ được dùng Java thuần, không phụ thuộc framework")
                .check(classes);
    }

    @Test
    void domainKhongPhuThuocTangKhac() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("..api..", "..infrastructure..", "..config..")
                .because("domain không được biết đến tầng API hay tầng dữ liệu")
                .check(classes);
    }

    @Test
    void apiKhongGoiThangTangDuLieu() {
        noClasses().that().resideInAPackage("..api..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .because("API chỉ gọi service của domain, không gọi thẳng tầng dữ liệu infrastructure")
                .check(classes);
    }

    @Test
    void infrastructureKhongGoiApi() {
        noClasses().that().resideInAPackage("..infrastructure..")
                .should().dependOnClassesThat().resideInAPackage("..api..")
                .check(classes);
    }
}
