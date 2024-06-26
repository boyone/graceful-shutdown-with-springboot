package com.example.greetingservice;
import com.example.greetingservice.exception.NotFoundException;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    // private Logger logger;

    @Autowired
    private UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        // this.logger = LoggerFactory.getLogger(UserService.class);
        this.userRepository = userRepository;
    }
    

    // Get all users
    public List<User> getAllUsers() {
        // logger.info("Fetching all users");
        var users = userRepository.findAll();
        return users;
    }

    // Get user by ID
    public User getUserById(Long id) {
        // logger.info("Fetching users by id: {}", id);
        return userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Invalid id: %d".formatted(id)));
    }

}