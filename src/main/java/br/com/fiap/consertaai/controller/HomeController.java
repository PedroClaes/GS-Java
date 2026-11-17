package br.com.fiap.consertaai.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Quem abre o link do deploy no navegador cai direto na documentacao da API. */
@Hidden
@Controller
public class HomeController {

    @GetMapping("/")
    public String raiz() {
        return "redirect:/swagger-ui.html";
    }
}
