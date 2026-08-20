package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.request.CreateDepartmentDto;
import com.example.employeetimetracking.dto.request.UpdateDepartmentDto;
import com.example.employeetimetracking.dto.response.DepartmentDto;
import com.example.employeetimetracking.exception.DepartmentAlreadyExistsException;
import com.example.employeetimetracking.exception.DepartmentNotFoundException;
import com.example.employeetimetracking.exception.InvalidTenantException;
import com.example.employeetimetracking.mapper.DepartmentMapper;
import com.example.employeetimetracking.model.entities.Company;
import com.example.employeetimetracking.model.entities.Department;
import com.example.employeetimetracking.repository.CompanyRepository;
import com.example.employeetimetracking.repository.DepartmentRepository;
import com.example.employeetimetracking.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final CompanyRepository companyRepository;
    private final DepartmentMapper departmentMapper;

    @Autowired
    public DepartmentService(DepartmentRepository departmentRepository,
                             CompanyRepository companyRepository,
                             DepartmentMapper departmentMapper) {
        this.departmentRepository = departmentRepository;
        this.companyRepository = companyRepository;
        this.departmentMapper = departmentMapper;
    }

    public Department getById(Long id) {
        return departmentRepository.findByIdAndCompanyId(id, currentCompanyId())
                .orElseThrow(() -> new DepartmentNotFoundException("Department does not exist"));
    }

    public Department getByDepartmentCode(String code) {
        String normalized = normalize(code);
        return departmentRepository.findByCompanyIdAndDepartmentCode(currentCompanyId(), normalized)
                .orElseThrow(() -> new DepartmentNotFoundException("Department doesn't exist"));
    }

    @Transactional(readOnly = true)
    public List<DepartmentDto> getAllDepartments() {
        return departmentRepository.findAllByCompanyId(currentCompanyId()).stream()
                .map(departmentMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public DepartmentDto getDepartmentById(Long id) {
        return departmentMapper.toDto(getById(id));
    }

    @Transactional
    public DepartmentDto create(CreateDepartmentDto request) {
        Long companyId = currentCompanyId();
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new InvalidTenantException("Tenant not found"));

        String name = normalize(request.getDepartmentName());
        String code = normalize(request.getDepartmentCode());
        assertUnique(companyId, name, code, null);

        Department department = new Department();
        department.setCompany(company);
        department.setDepartmentName(name);
        department.setDepartmentCode(code);
        department.setIsActive(true);
        return departmentMapper.toDto(departmentRepository.save(department));
    }

    @Transactional
    public DepartmentDto update(Long id, UpdateDepartmentDto request) {
        Department department = getById(id);
        Long companyId = currentCompanyId();

        String name = department.getDepartmentName();
        if (request.getDepartmentName() != null && !normalize(request.getDepartmentName()).isBlank()) {
            name = normalize(request.getDepartmentName());
        }
        String code = department.getDepartmentCode();
        if (request.getDepartmentCode() != null && !normalize(request.getDepartmentCode()).isBlank()) {
            code = normalize(request.getDepartmentCode());
        }
        assertUnique(companyId, name, code, department.getId());

        department.setDepartmentName(name);
        department.setDepartmentCode(code);
        if (request.getIsActive() != null) {
            department.setIsActive(request.getIsActive());
        }
        return departmentMapper.toDto(department);
    }

    private void assertUnique(Long companyId, String name, String code, Long excludeId) {
        boolean codeTaken = excludeId == null
                ? departmentRepository.existsByCompanyIdAndDepartmentCode(companyId, code)
                : departmentRepository.existsByCompanyIdAndDepartmentCodeAndIdNot(companyId, code, excludeId);
        if (codeTaken) {
            throw new DepartmentAlreadyExistsException("Department code already in use: " + code);
        }
        boolean nameTaken = excludeId == null
                ? departmentRepository.existsByCompanyIdAndDepartmentName(companyId, name)
                : departmentRepository.existsByCompanyIdAndDepartmentNameAndIdNot(companyId, name, excludeId);
        if (nameTaken) {
            throw new DepartmentAlreadyExistsException("Department name already in use: " + name);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static Long currentCompanyId() {
        return TenantContext.require().companyId();
    }
}
