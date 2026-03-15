package com.example.employeetimetracking.specification;

import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.UserRole;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecifications {

    public static Specification<User> hasDepartmentId(Long id){
        return (root,query,cb) -> id == null ? cb.conjunction() : cb.equal(root.get("department").get("id") , id);
    }

    public static Specification<User> hasRole(UserRole role){
        return (root, query, cb) -> role == null ? cb.conjunction() : cb.equal(root.get("userRole") , role);
    }

    public static Specification<User> isActive(Boolean isActive){
        return (root, query, cb) -> isActive == null ? cb.conjunction() : cb.equal(root.get("isActive") , isActive);
    }

    public static Specification<User> hasName(String name){
        return (root, query, cb) -> name == null ? cb.conjunction() : cb.or(
                cb.like(cb.lower(root.get("firstName")), "%" + name.toLowerCase() + "%"),
                cb.like(cb.lower(root.get("lastName")), "%" + name.toLowerCase() + "%")
        );
    }


}
