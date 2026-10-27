package pe.com.anterza.gestion.ventas.inventario.controlador;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Pagina de inicio: al entrar a "/" se abre Registrar Compra. */
@Controller
public class InicioController {

    @GetMapping("/")
    public String inicio() {
        return "redirect:/compras";
    }
}