package com.mariem.assurance;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;


@EnableAsync
@SpringBootApplication
public class AssuranceNetworkApiApplication {

	// 🔧 Ajout important : permet de redéfinir les beans
	static {
		System.setProperty("spring.main.allow-bean-definition-overriding", "true");
	}

	public static void main(String[] args) {
		SpringApplication.run(AssuranceNetworkApiApplication.class, args);
	}

	//@Bean
    /*public CommandLineRunner runner(RoleRepository roleRepository) {
        return args -> {
            if (roleRepository.findByName("USER").isEmpty()) {
                roleRepository.save(Role.builder().name("USER").build());
            }
        };
    }*/
}




	//@Bean
	/*public CommandLineRunner runner(RoleRepository roleRepository) {
		return args -> {
			if (roleRepository.findByName("USER").isEmpty()) {
				roleRepository.save(Role.builder().name("USER").build());
			}
		};
	}*/

