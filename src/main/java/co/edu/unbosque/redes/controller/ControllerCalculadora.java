package co.edu.unbosque.redes.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import co.edu.unbosque.redes.modelo.CalculadoraIPV4;

@Controller
public class ControllerCalculadora {

	@Autowired
	private CalculadoraIPV4 calculadora;

	@GetMapping
	public String calculadoraIPV4() {
		return "index";

	}

	@PostMapping("/calculadora")
	public String calculo(@RequestParam("ip") String ip, @RequestParam("mascara") int mascara, Model model) {

			if(calculadora.calcularRedInfo(ip, mascara)) {
				model.addAttribute("direccion_ip", calculadora.getIp());
				model.addAttribute("mascara_red", calculadora.getMascara());
				model.addAttribute("direccion_red", calculadora.getDireccion_red());
				model.addAttribute("broadcast", calculadora.getBroadcast());
				model.addAttribute("numerohost", calculadora.getNumero_host() + ", Rango de IP utiles " + calculadora.getRangoIPs());
				model.addAttribute("clase_ip", calculadora.getClaseIP());
				model.addAttribute("tipoIp", calculadora.getTipoIP());
				model.addAttribute("tipoIp", calculadora.getTipoIP());
				model.addAttribute("porcion_red", calculadora.getParte_red_binaria());
				model.addAttribute("porcion_host", calculadora.getParte_host_binaria());
				return "index";
			}else{
				model.addAttribute("direccion_ip", "Direccion de ip invalidad");
				model.addAttribute("mascara_red", "0");
				model.addAttribute("direccion_red","0");
				model.addAttribute("broadcast","0");
				model.addAttribute("numerohost", "0");
				model.addAttribute("clase_ip","0");
				model.addAttribute("tipoIp", "0");
				model.addAttribute("tipoIp", "0");
				model.addAttribute("porcion_red", "0");
				model.addAttribute("porcion_host", "0");
				return "index";
			}






	}

}
