package inventory.services.impl;


import com.mysql.cj.util.StringUtils;
import inventory.POJO.User;
import inventory.constants.InventoryConstents;
import inventory.constants.Roles;
import inventory.dao.UserDao;
import inventory.filters.CustomerUserDetailsService;
import inventory.filters.JwtFilter;
import inventory.filters.JwtUtil;
import inventory.services.UserService;
import inventory.utils.EmailUtils;
import inventory.utils.InventoryUtils;
import inventory.wrapper.UserWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    UserDao userDao;

    AuthenticationManager authenticationManager;

    CustomerUserDetailsService customerUserDetailsService;

    JwtUtil jwtUtil;

    JwtFilter jwtFilter;

    EmailUtils emailUtils;

    @Autowired
    public UserServiceImpl(UserDao userDao, AuthenticationManager authenticationManager,
                           CustomerUserDetailsService customerUserDetailsService, JwtUtil jwtUtil, JwtFilter jwtFilter,
                           EmailUtils emailUtils) {
        this.userDao = userDao;
        this.authenticationManager = authenticationManager;
        this.customerUserDetailsService = customerUserDetailsService;
        this.jwtUtil = jwtUtil;
        this.jwtFilter = jwtFilter;
        this.emailUtils = emailUtils;
    }

    @Override
    public ResponseEntity<String> signUp(Map<String, String> requestMap) {
        log.info("Inside signup {}", requestMap);
        try {
            if (validateSignUpMap(requestMap)) {
                User users = userDao.findByEmailId((requestMap.get("email")));
                if (Objects.isNull(users)) {
                    userDao.save(getUserFromMap(requestMap));
                    return InventoryUtils.getResponseEntity("Successfully Registered", HttpStatus.OK);
                } else {
                    return InventoryUtils.getResponseEntity("Email already exits", HttpStatus.BAD_REQUEST);
                }
            } else {
                return InventoryUtils.getResponseEntity(InventoryConstents.INVALID_DATA, HttpStatus.BAD_REQUEST);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return InventoryUtils.getResponseEntity(InventoryConstents.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private boolean validateSignUpMap(Map<String, String> requestMap) {
        if (requestMap.containsKey("name") && requestMap.containsKey("contactNumber") && requestMap.containsKey("email") && requestMap.containsKey("password")) {
            return true;
        }
        return false;
    }

    private User getUserFromMap(Map<String, String> requestMap) {
        User user = new User();
        user.setName(requestMap.get("name"));
        user.setContactNumber(requestMap.get("contactNumber"));
        user.setEmail(requestMap.get("email"));
        user.setPassword(requestMap.get("password"));
        user.setStatus("false");
        user.setRole("user");
        return user;
    }

    @Override
    public ResponseEntity<String> login(Map<String, String> requestMap) {
        log.info("Inside login");
        try {
            Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(requestMap.get("email"), requestMap.get("password")));
            if (auth.isAuthenticated()) {
                if (customerUserDetailsService.getUserDetail().getStatus().equalsIgnoreCase("true")) {
                    return new ResponseEntity<String>("{\"token\":\"" + jwtUtil.generateToken(customerUserDetailsService.getUserDetail().getEmail(), customerUserDetailsService.getUserDetail().getRole()) + "\"}", HttpStatus.OK);
                } else {
                    return new ResponseEntity<String>("{\"message\":\"" + "Wait for admin approval." + "\"}", HttpStatus.BAD_REQUEST);
                }
            }
        } catch (Exception ex) {
            log.error("{}", ex);
        }
        return new ResponseEntity<String>("{\"message\":\"" + "Bad Credentials." + "\"}", HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<List<UserWrapper>> getAllUser() {
        try {
            if (jwtFilter.isAdmin()) {
                return new ResponseEntity<>(userDao.getAllUser(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> update(Map<String, String> requestMap) {
        try {
            if (jwtFilter.isAdmin()) {
                Optional<User> optional = userDao.findById(Integer.parseInt(requestMap.get("id")));
                if (!optional.isPresent()) {
                    InventoryUtils.getResponseEntity("User id does not exits", HttpStatus.OK);
                } else {
                    userDao.updateStatus(requestMap.get("status"), Integer.parseInt(requestMap.get("id")));
                    sendMailTOAllAdmin(requestMap.get("status"), optional.get().getEmail(), userDao.findUsersByRole(Roles.ADMIN.name()));
                    return InventoryUtils.getResponseEntity("User Status updated successfully", HttpStatus.OK);
                }
            } else {
                return InventoryUtils.getResponseEntity(InventoryConstents.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return InventoryUtils.getResponseEntity(InventoryConstents.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void sendMailTOAllAdmin(String status, String user, List<String> allAdmin) {
        allAdmin.remove(jwtFilter.getCurrentUser());
        if (status != null && status.equalsIgnoreCase("true")) {
            emailUtils.sendSimpleMessage(jwtFilter.getCurrentUser(), "Account approved ", "USER:- " + user + "\n is approved by \n ADMIN: " + jwtFilter.getCurrentUser(), allAdmin);
        } else {
            emailUtils.sendSimpleMessage(jwtFilter.getCurrentUser(), "Account disabled ", "USER:- " + user + "\n is disabled by \n ADMIN: " + jwtFilter.getCurrentUser(), allAdmin);
        }
    }

    @Override
    public ResponseEntity<String> checkToken() {
        return InventoryUtils.getResponseEntity("true", HttpStatus.OK);
    }

    @Override
    public ResponseEntity<String> changePassword(Map<String, String> requestMap) {
        try {
            User usersObj = userDao.findByEmailId(jwtFilter.getCurrentUser());
            if (!usersObj.equals(null)) {
                if (usersObj.getPassword().equals(requestMap.get("oldPassword"))) {
                    usersObj.setPassword(requestMap.get("newPassword"));
                    userDao.save(usersObj);
                    return InventoryUtils.getResponseEntity("Password updated successfully", HttpStatus.OK);
                }
                return InventoryUtils.getResponseEntity("Incorrect Old Password", HttpStatus.BAD_REQUEST);
            }
            return InventoryUtils.getResponseEntity(InventoryConstents.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return InventoryUtils.getResponseEntity(InventoryConstents.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> forgotPassword(Map<String, String> requestMap) {
        try {
            User users = userDao.findByEmailId(requestMap.get("email"));
            if (!Objects.isNull(users) && !StringUtils.isNullOrEmpty(users.getEmail())) {
                emailUtils.forgotMail(users.getEmail(), "Credentials by Inventory System", users.getPassword());
            }
            return InventoryUtils.getResponseEntity("Check your mail for Credentials", HttpStatus.OK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return InventoryUtils.getResponseEntity(InventoryConstents.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}