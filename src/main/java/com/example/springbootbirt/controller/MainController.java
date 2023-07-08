package com.example.springbootbirt.controller;

import com.example.springbootbirt.dto.OutputType;
import com.example.springbootbirt.dto.Report;
import com.example.springbootbirt.service.ReportService;
import com.ibm.icu.util.ULocale;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.birt.core.framework.Platform;
import org.eclipse.birt.report.model.api.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MainController {
    private final ReportService service;

    /**
     * Получение списка доступных шаблонов отчета из каталога "reports"
     * @return      список названий шаблонов отчетов
     */
    @GetMapping("/")
    public List<String> getListOfReports() {
        List<Report> reports = service.getReports();
        log.debug("List of current reports: {}", reports);
        return reports.stream().map(Report::getName).toList();
    }

    /**
     * Генерирование отчета по названию шаблона и формату результата
     * @param request       запрос на получение отчета
     * @param response      ответ на запрос
     * @param name          название шаблона отчета
     * @param output        тип формата
     */
    @GetMapping("/report/{name}")
    public void generateFullReport(HttpServletRequest request, HttpServletResponse response,
                                   @PathVariable("name") String name, @RequestParam("output") String output) {
        OutputType format = OutputType.from(output);
        if (format.equals(OutputType.INVALID)) {
            log.error("Invalid name of type of report: {}", output);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } else {
            log.info("Generating full report: {}, format: {}", name, format);
            service.generateMainReport(name, format, request, response);
        }
    }

}
