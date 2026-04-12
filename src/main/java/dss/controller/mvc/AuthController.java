package dss.controller.mvc;


import dss.dto.UserDto;
import dss.model.entity.User;
import dss.repository.RoleRepository;
import dss.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@AllArgsConstructor
public class AuthController {

    private UserService userService;
    private RoleRepository roleRepository;


    // handler method to handle home page request
    @GetMapping({"/", "/index"})
    public String home(){
        return "redirect:/login";
    }

    // handler method to handle login request
    @GetMapping("/login")
    public String login(){
        return "login.html";
    }

    // handler method to handle user registration form request
    @GetMapping("/register")
    public String showRegistrationForm(Model model){
        System.out.println("inside register method");
        // create model object to store form data
        UserDto user = new UserDto();
        model.addAttribute("user", user);
        return "register";
    }

    // handler method to handle user registration form submit request
    @PostMapping("/register/save")
    public String registration(@Valid @ModelAttribute("user") UserDto userDto,
                               BindingResult result,
                               Model model){
        User existingUser = userService.findUserByEmail(userDto.getEmail());

        if(existingUser != null && existingUser.getEmail() != null && !existingUser.getEmail().isEmpty()){
            result.rejectValue("email", null,
                    "There is already an account registered with the same email");
        }

        if(result.hasErrors()){
            model.addAttribute("user", userDto);
            return "/register";
        }

        userService.saveUser(userDto);
        System.out.println("user saved");
        return "redirect:/register?success";
    }

    // handler method to handle list of users
    @GetMapping("/users")
    public String users(Model model){
        List<User> users = userService.getAllUsers()
                .stream()
                .sorted(Comparator.comparing(User::getId).reversed())
                .collect(Collectors.toList());

        model.addAttribute("users", users);
        model.addAttribute("roles",roleRepository.findAll());
        return "users";
    }

    @GetMapping("/profile")
    public String profile(Model model, Authentication auth){
        model.addAttribute("user", userService.findUserByEmail(auth.getName()));

        return "profile";
    }
}
