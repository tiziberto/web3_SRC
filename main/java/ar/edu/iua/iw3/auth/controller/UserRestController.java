package ar.edu.iua.iw3.auth.controller;

import java.util.HashSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.iua.iw3.auth.IUserBusiness;
import ar.edu.iua.iw3.auth.RoleRepository;
import ar.edu.iua.iw3.auth.User;
import ar.edu.iua.iw3.controllers.BaseRestController;
import ar.edu.iua.iw3.controllers.Constants;
import ar.edu.iua.iw3.util.IStandartResponseBusiness;
import java.util.Set;
import java.util.HashSet;
import ar.edu.iua.iw3.auth.Role;

@RestController
@RequestMapping(Constants.URL_BASE + "/users")
public class UserRestController extends BaseRestController {

    @Autowired
    private IUserBusiness userBusiness;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private IStandartResponseBusiness response;

    // Listar todos (Solo Admin)
    @GetMapping("")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> list() {
        try {
            return new ResponseEntity<>(userBusiness.list(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(response.build(HttpStatus.INTERNAL_SERVER_ERROR, e, e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Autowired
    private RoleRepository roleRepository;
    // Editar Usuario (Roles, Password, etc)
    @PutMapping("/update")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> updateUser(@RequestBody User user) {
        try {
            User original = userBusiness.load(user.getUsername());
            
            if(user.getPassword() != null && !user.getPassword().isEmpty()) {
                original.setPassword(user.getPassword());
            }
            
            // CORRECCIÓN AQUÍ:
            if(user.getRoles() != null && !user.getRoles().isEmpty()) {
                Set<Role> rolesPersistentes = new HashSet<>();
                for(Role r : user.getRoles()) {
                    // Buscamos el rol real en la BD para que Hibernate esté feliz
                    roleRepository.findByName(r.getName()).ifPresent(rolesPersistentes::add);
                }
                original.setRoles(rolesPersistentes);
            }
            
            original.setEnabled(user.isEnabled());
            
            userBusiness.save(original, passwordEncoder);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(response.build(HttpStatus.INTERNAL_SERVER_ERROR, e, e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}