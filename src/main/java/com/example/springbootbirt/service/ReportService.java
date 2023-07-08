package com.example.springbootbirt.service;

import com.example.springbootbirt.dto.OutputType;
import com.example.springbootbirt.dto.Parameter;
import com.example.springbootbirt.dto.ParameterType;
import com.example.springbootbirt.dto.Report;
import com.ibm.icu.util.ULocale;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.framework.Platform;
import org.eclipse.birt.report.engine.api.*;
import org.eclipse.birt.report.model.api.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

@Service
public class ReportService implements ApplicationContextAware, DisposableBean {
    private final HTMLServerImageHandler htmlImageHandler = new HTMLServerImageHandler();

    @Value("${reports.relative.path}")
    private String reportsPath;
    @Value("${images.relative.path}")
    private String imagesPath;
    private IReportEngine birtEngine;
    private ApplicationContext context;
    private String imageFolder;
    private Map<String, IReportRunnable> reports = new HashMap<>();

    @PostConstruct
    protected void initialize() throws BirtException {
        EngineConfig config = new EngineConfig();
        Platform.startup(config);
        IReportEngineFactory factory = (IReportEngineFactory) Platform
            .createFactoryObject(IReportEngineFactory.EXTENSION_REPORT_ENGINE_FACTORY);
        birtEngine = factory.createReportEngine(config);
        imageFolder = System.getProperty("user.dir") + File.separatorChar + reportsPath + imagesPath;
        loadReports();
    }

    public void loadReports() throws EngineException {
        File folder = new File(reportsPath);
        for (String file : Objects.requireNonNull(folder.list())) {
            if (!file.endsWith(".rptdesign")) {
                continue;
            }
            reports.put(file.replace(".rptdesign", ""),
                birtEngine.openReportDesign(folder.getAbsolutePath() + File.separator + file));
        }
    }

    public List<Report> getReports() {
        List<Report> response = new ArrayList<>();
        for (Map.Entry<String, IReportRunnable> entry : reports.entrySet()) {
            IReportRunnable report = reports.get(entry.getKey());
            IGetParameterDefinitionTask task = birtEngine.createGetParameterDefinitionTask(report);
            Report reportItem = new Report(report.getDesignHandle().getProperty("title").toString(), entry.getKey());
            for (Object h : task.getParameterDefns(false)) {
                IParameterDefn def = (IParameterDefn) h;
                reportItem.getParameters()
                    .add(new Parameter(def.getPromptText(), def.getName(), getParameterType(def)));
            }
            response.add(reportItem);
        }
        return response;
    }

    private ParameterType getParameterType(IParameterDefn param) {
        if (IParameterDefn.TYPE_INTEGER == param.getDataType()) {
            return ParameterType.INT;
        }
        return ParameterType.STRING;
    }

    /**
     * Создание отчета в зависимости от выбранного типа
     * @param reportName        название шаблона
     * @param output            формат отчета
     * @param request           запрос на создание отчета
     * @param response          ответ на запрос
     */
    public void generateMainReport(String reportName, OutputType output,
                                   HttpServletRequest request, HttpServletResponse response) {
        switch (output) {
            case HTML -> generateHTMLReport(reports.get(reportName), request, response);
            case PDF -> generatePDFReport(reports.get(reportName), request, response);
            case XLSX -> generateXLSXReport(reports.get(reportName), request, response);
            default -> throw new IllegalArgumentException("Output type not recognized:" + output);
        }
    }

    /**
     * Создание отчета в HTML
     */
    private void generateHTMLReport(IReportRunnable report, HttpServletRequest request, HttpServletResponse response) {
        HTMLRenderOption htmlOptions = new HTMLRenderOption();
        htmlOptions.setOutputFormat("html");
        htmlOptions.setBaseImageURL("/" + reportsPath + imagesPath);
        htmlOptions.setImageDirectory(imageFolder);
        htmlOptions.setImageHandler(htmlImageHandler);

        response.setContentType(birtEngine.getMIMEType("html"));

        generateReportWithType(report, htmlOptions, request, response);
    }

    /**
     * Создание отчета в PDF
     */
    private void generatePDFReport(IReportRunnable report, HttpServletRequest request, HttpServletResponse response) {
        PDFRenderOption pdfRenderOption = new PDFRenderOption();
        pdfRenderOption.setOutputFormat("pdf");

        response.setContentType(birtEngine.getMIMEType("pdf"));

        generateReportWithType(report, pdfRenderOption, request, response);
    }

    /**
     * Создание отчета в XLSX
     */
    private void generateXLSXReport(IReportRunnable report, HttpServletRequest request, HttpServletResponse response) {
        EXCELRenderOption excelRenderOption = new EXCELRenderOption();
        excelRenderOption.setOutputFormat("xlsx");

        response.setContentType(birtEngine.getMIMEType("xlsx"));
        response.setContentType("application/xlsx");
        response.setHeader("Content-Disposition", "filename=report.xlsx");


        generateReportWithType(report, excelRenderOption, request, response);
    }

    private void generateReportWithType(IReportRunnable report, RenderOption option,
                                        HttpServletRequest request, HttpServletResponse response) {
        IRunAndRenderTask runAndRenderTask = birtEngine.createRunAndRenderTask(report);
        runAndRenderTask.setRenderOption(option);
//        runAndRenderTask.getAppContext().put(option.get, request);

        try {
            option.setOutputStream(response.getOutputStream());
            runAndRenderTask.run();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            runAndRenderTask.close();
        }
    }

    @Override
    public void destroy() {
        birtEngine.destroy();
        Platform.shutdown();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }


    @SneakyThrows
    private void createStaticReport(){
        final DesignConfig config = new DesignConfig();

        final IDesignEngine engine;
        try {
            Platform.startup(config);
            IDesignEngineFactory factory = (IDesignEngineFactory) Platform
                .createFactoryObject(IDesignEngineFactory.EXTENSION_DESIGN_ENGINE_FACTORY);
            engine = factory.createDesignEngine(config);

        } catch (Exception ex) {
//            log.error("Exception during creation of DesignEngine", ex);
            throw ex;
        }

        SessionHandle session = engine.newSessionHandle(ULocale.ENGLISH);
        ReportDesignHandle design = session.createDesign();
        design.setTitle("Sample Report");
        ElementFactory factory = design.getElementFactory();
        DesignElementHandle element = factory.newSimpleMasterPage("Page Master");
        design.getMasterPages().add(element);

        GridHandle grid = factory.newGridItem(null, 2, 1);
        design.getBody().add(grid);

        grid.setWidth("100%");

        RowHandle row0 = (RowHandle) grid.getRows().get(0);

        ImageHandle image = factory.newImage(null);
        CellHandle cell = (CellHandle) row0.getCells().get(0);
        cell.getContent().add(image);
        image.setURL("\"https://www.baeldung.com/wp-content/themes/baeldung/favicon/favicon-96x96.png\"");

        LabelHandle label = factory.newLabel(null);
        cell = (CellHandle) row0.getCells().get(1);
        cell.getContent().add(label);
        label.setText("Hello, Baeldung world!");
    }
}
