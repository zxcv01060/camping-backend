package tw.edu.ntub.imd.camping.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(path = "/problem-report/manager")
public class ProblemReportRouteController {

    @GetMapping(path = "")
    public ModelAndView indexPage() {
        return new ModelAndView("/problem-report");
    }
}
