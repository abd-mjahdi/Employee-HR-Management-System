package com.example.employeetimetracking.unit.dto;

import com.example.employeetimetracking.config.IgnoreClientCompanyIdMixin;
import com.example.employeetimetracking.dto.request.CreateDepartmentDto;
import com.example.employeetimetracking.dto.request.LoginRequestDto;
import com.example.employeetimetracking.dto.response.BootstrapCompanyResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class PublicDtoCompanyIdTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .addMixIn(Object.class, IgnoreClientCompanyIdMixin.class);

    @Test
    void dtoClasses_doNotDeclareCompanyId() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*Dto")));
        Set<BeanDefinition> beans = scanner.findCandidateComponents(
                "com.example.employeetimetracking.dto");
        assertFalse(beans.isEmpty());

        for (BeanDefinition bean : beans) {
            Class<?> type = Class.forName(bean.getBeanClassName());
            for (Field field : type.getDeclaredFields()) {
                String name = field.getName();
                assertFalse(
                        "companyId".equals(name) || "company_id".equals(name),
                        type.getName() + " must not expose " + name);
            }
        }
    }

    @Test
    void requestJson_ignoresClientCompanyId() throws Exception {
        CreateDepartmentDto department = mapper.readValue(
                """
                {"departmentName":"Ops","departmentCode":"OPS","companyId":99,"company_id":88}
                """,
                CreateDepartmentDto.class);
        assertEquals("Ops", department.getDepartmentName());
        assertEquals("OPS", department.getDepartmentCode());

        LoginRequestDto login = mapper.readValue(
                """
                {"email":"hr@acme.com","password":"secret1","companyId":99}
                """,
                LoginRequestDto.class);
        assertEquals("hr@acme.com", login.getEmail());
        assertEquals("secret1", login.getPassword());
    }

    @Test
    void bootstrapResponse_doesNotSerializeCompanyId() throws Exception {
        BootstrapCompanyResponseDto response = new BootstrapCompanyResponseDto(
                "Acme", "acme", "hr@acme.com", null, "acme.myhr.com");
        String json = mapper.writeValueAsString(response);
        assertFalse(json.contains("companyId"));
        assertFalse(json.contains("company_id"));
        assertNull(response.getTemporaryPassword());
    }
}
