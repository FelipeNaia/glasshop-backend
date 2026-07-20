package com.example.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateRole(@PathVariable String id, @RequestBody UpdateRoleRequest request) {
        return userRepository.findById(id).map(user -> {
            user.setRole(request.getRole());
            User saved = userRepository.save(user);
            return ResponseEntity.ok(new UserResponse(saved.getId(), saved.getEmail(), saved.getRole()));
        }).orElse(ResponseEntity.notFound().build());
    }
}
