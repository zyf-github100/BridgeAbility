package com.rongzhiqiao.jobseeker.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import com.rongzhiqiao.jobseeker.vo.JobseekerAbilityCardResponse;
import com.rongzhiqiao.jobseeker.vo.JobseekerProfileResponse;
import com.rongzhiqiao.jobseeker.vo.JobseekerProjectExperienceResponse;
import com.rongzhiqiao.jobseeker.vo.JobseekerResumePreviewResponse;
import com.rongzhiqiao.jobseeker.vo.JobseekerSkillTagResponse;
import com.rongzhiqiao.jobseeker.vo.JobseekerSupportNeedResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobseekerResumeExporter {

    private static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String DEFAULT_DOC_FONT = "Microsoft YaHei";
    private static final List<String> PDF_FONT_CANDIDATES = List.of(
            "C:\\Windows\\Fonts\\msyh.ttc,0",
            "C:\\Windows\\Fonts\\msyhbd.ttc,0",
            "C:\\Windows\\Fonts\\simhei.ttf",
            "C:\\Windows\\Fonts\\simsun.ttc,0",
            "C:\\Windows\\Fonts\\simsunb.ttf"
    );

    public ResumeExportPayload export(JobseekerResumePreviewResponse preview, String baseName, String requestedFormat) {
        ResumeExportFormat format = ResumeExportFormat.from(requestedFormat);
        return switch (format) {
            case PDF -> new ResumeExportPayload(exportPdf(preview), baseName + ".pdf", PDF_CONTENT_TYPE);
            case DOCX -> new ResumeExportPayload(exportDocx(preview), baseName + ".docx", DOCX_CONTENT_TYPE);
        };
    }

    private byte[] exportPdf(JobseekerResumePreviewResponse preview) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            Document document = new Document(PageSize.A4, 48, 48, 54, 54);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            PdfFontBundle fonts = createPdfFontBundle();
            addPdfTitle(document, fonts, preview.displayName(), preview.headline(), preview.generatedAt());
            addPdfSection(document, fonts, "个人简介");
            addPdfParagraph(document, fonts.body(), preview.summary());

            addPdfSection(document, fonts, "基本信息");
            addPdfBulletList(document, fonts, buildBasicInfo(preview.profile()));

            addPdfSection(document, fonts, "核心技能");
            addPdfBulletList(document, fonts, buildSkillLines(preview.profile()));

            addPdfSection(document, fonts, "项目经历");
            addPdfProjectLines(document, fonts, preview.profile());

            addPdfSection(document, fonts, "能力亮点");
            addPdfAbilityLines(document, fonts, preview.profile(), preview.strengths());

            List<String> accessibilityLines = buildAccessibilityLines(preview.supportNeeds());
            if (!accessibilityLines.isEmpty()) {
                addPdfSection(document, fonts, "无障碍沟通与协作");
                addPdfBulletList(document, fonts, accessibilityLines);
            }

            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Failed to export resume PDF", ex);
        }
    }

    private byte[] exportDocx(JobseekerResumePreviewResponse preview) {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            addDocxTitle(document, preview.displayName(), preview.headline(), preview.generatedAt());
            addDocxSection(document, "个人简介");
            addDocxParagraph(document, preview.summary(), false, 22);

            addDocxSection(document, "基本信息");
            addDocxBulletLines(document, buildBasicInfo(preview.profile()));

            addDocxSection(document, "核心技能");
            addDocxBulletLines(document, buildSkillLines(preview.profile()));

            addDocxSection(document, "项目经历");
            addDocxProjects(document, preview.profile());

            addDocxSection(document, "能力亮点");
            addDocxAbilities(document, preview.profile(), preview.strengths());

            List<String> accessibilityLines = buildAccessibilityLines(preview.supportNeeds());
            if (!accessibilityLines.isEmpty()) {
                addDocxSection(document, "无障碍沟通与协作");
                addDocxBulletLines(document, accessibilityLines);
            }

            document.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to export resume Word document", ex);
        }
    }

    private void addPdfTitle(Document document, PdfFontBundle fonts, String name, String headline, String generatedAt)
            throws DocumentException {
        Paragraph title = new Paragraph(name, fonts.title());
        title.setSpacingAfter(8f);
        document.add(title);

        Paragraph subtitle = new Paragraph(headline, fonts.heading());
        subtitle.setSpacingAfter(4f);
        document.add(subtitle);

        Paragraph meta = new Paragraph("导出时间：" + generatedAt, fonts.meta());
        meta.setSpacingAfter(18f);
        document.add(meta);
    }

    private void addPdfSection(Document document, PdfFontBundle fonts, String title) throws DocumentException {
        Paragraph paragraph = new Paragraph(title, fonts.heading());
        paragraph.setSpacingBefore(8f);
        paragraph.setSpacingAfter(8f);
        document.add(paragraph);
    }

    private void addPdfParagraph(Document document, Font font, String text) throws DocumentException {
        Paragraph paragraph = new Paragraph(valueOrDefault(text), font);
        paragraph.setSpacingAfter(10f);
        document.add(paragraph);
    }

    private void addPdfBulletList(Document document, PdfFontBundle fonts, List<String> lines) throws DocumentException {
        List<String> safeLines = lines.isEmpty() ? List.of("待补充") : lines;
        for (String line : safeLines) {
            Paragraph paragraph = new Paragraph(new Phrase("• " + line, fonts.body()));
            paragraph.setIndentationLeft(10f);
            paragraph.setSpacingAfter(4f);
            document.add(paragraph);
        }
    }

    private void addPdfProjectLines(Document document, PdfFontBundle fonts, JobseekerProfileResponse profile)
            throws DocumentException {
        if (profile == null || profile.getProjectExperiences() == null || profile.getProjectExperiences().isEmpty()) {
            addPdfBulletList(document, fonts, List.of("待补充项目经历"));
            return;
        }

        for (JobseekerProjectExperienceResponse project : profile.getProjectExperiences()) {
            Paragraph title = new Paragraph(project.projectName() + " / " + valueOrDefault(project.roleName()), fonts.bodyBold());
            title.setSpacingAfter(2f);
            document.add(title);
            addPdfBulletList(document, fonts, List.of(
                    "时间：" + valueOrDefault(project.periodLabel()),
                    "内容：" + valueOrDefault(project.description())
            ));
        }
    }

    private void addPdfAbilityLines(Document document,
                                    PdfFontBundle fonts,
                                    JobseekerProfileResponse profile,
                                    List<String> strengths) throws DocumentException {
        if (profile != null && profile.getAbilityCards() != null && !profile.getAbilityCards().isEmpty()) {
            for (JobseekerAbilityCardResponse card : profile.getAbilityCards()) {
                Paragraph title = new Paragraph(card.title(), fonts.bodyBold());
                title.setSpacingAfter(2f);
                document.add(title);
                addPdfBulletList(document, fonts, merge(card.summary(), card.highlights()));
            }
            return;
        }
        addPdfBulletList(document, fonts, strengths);
    }

    private void addDocxTitle(XWPFDocument document, String name, String headline, String generatedAt) {
        XWPFParagraph title = document.createParagraph();
        title.setAlignment(ParagraphAlignment.LEFT);
        createRun(title, name, true, 34);

        XWPFParagraph subtitle = document.createParagraph();
        subtitle.setSpacingAfter(120);
        createRun(subtitle, headline, true, 24);

        XWPFParagraph meta = document.createParagraph();
        meta.setSpacingAfter(220);
        createRun(meta, "导出时间：" + generatedAt, false, 20);
    }

    private void addDocxSection(XWPFDocument document, String title) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingBefore(160);
        paragraph.setSpacingAfter(80);
        createRun(paragraph, title, true, 24);
    }

    private void addDocxParagraph(XWPFDocument document, String text, boolean bold, int size) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingAfter(120);
        createRun(paragraph, valueOrDefault(text), bold, size);
    }

    private void addDocxBulletLines(XWPFDocument document, List<String> lines) {
        List<String> safeLines = lines.isEmpty() ? List.of("待补充") : lines;
        for (String line : safeLines) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.setSpacingAfter(50);
            createRun(paragraph, "• " + line, false, 21);
        }
    }

    private void addDocxProjects(XWPFDocument document, JobseekerProfileResponse profile) {
        if (profile == null || profile.getProjectExperiences() == null || profile.getProjectExperiences().isEmpty()) {
            addDocxBulletLines(document, List.of("待补充项目经历"));
            return;
        }

        for (JobseekerProjectExperienceResponse project : profile.getProjectExperiences()) {
            addDocxParagraph(document, project.projectName() + " / " + valueOrDefault(project.roleName()), true, 21);
            addDocxBulletLines(document, List.of(
                    "时间：" + valueOrDefault(project.periodLabel()),
                    "内容：" + valueOrDefault(project.description())
            ));
        }
    }

    private void addDocxAbilities(XWPFDocument document, JobseekerProfileResponse profile, List<String> strengths) {
        if (profile != null && profile.getAbilityCards() != null && !profile.getAbilityCards().isEmpty()) {
            for (JobseekerAbilityCardResponse card : profile.getAbilityCards()) {
                addDocxParagraph(document, card.title(), true, 21);
                addDocxBulletLines(document, merge(card.summary(), card.highlights()));
            }
            return;
        }
        addDocxBulletLines(document, strengths);
    }

    private void createRun(XWPFParagraph paragraph, String text, boolean bold, int size) {
        XWPFRun run = paragraph.createRun();
        run.setFontFamily(DEFAULT_DOC_FONT);
        run.setFontSize(size);
        run.setBold(bold);
        run.setText(valueOrDefault(text));
    }

    private List<String> buildBasicInfo(JobseekerProfileResponse profile) {
        if (profile == null) {
            return List.of("目标岗位：待补充", "目标城市：待补充", "教育经历：待补充");
        }
        return List.of(
                "目标岗位：" + valueOrDefault(profile.getExpectedJob()),
                "目标城市：" + valueOrDefault(profile.getTargetCity()),
                "工作方式：" + workModeLabel(profile.getWorkModePreference()),
                "学校：" + valueOrDefault(profile.getSchoolName()),
                "专业：" + valueOrDefault(profile.getMajor()),
                "学历：" + valueOrDefault(profile.getDegree()),
                "毕业年份：" + valueOrDefault(profile.getGraduationYear())
        );
    }

    private List<String> buildSkillLines(JobseekerProfileResponse profile) {
        if (profile == null || profile.getSkillTags() == null || profile.getSkillTags().isEmpty()) {
            return List.of("待补充核心技能");
        }
        return profile.getSkillTags().stream()
                .map(skill -> skill.skillName() + " / " + skill.skillLevelLabel())
                .toList();
    }

    private List<String> buildAccessibilityLines(JobseekerSupportNeedResponse supportNeeds) {
        if (supportNeeds == null || !"SUMMARY".equalsIgnoreCase(valueOrDefault(supportNeeds.supportVisibility()))) {
            return List.of();
        }
        if (supportNeeds.supportSummary() == null || supportNeeds.supportSummary().isEmpty()) {
            return List.of("已授权展示协作与便利需求摘要");
        }
        return supportNeeds.supportSummary();
    }

    private List<String> merge(String summary, List<String> highlights) {
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        if (hasText(summary)) {
            lines.add(summary.trim());
        }
        if (highlights != null) {
            highlights.stream().filter(this::hasText).map(String::trim).forEach(lines::add);
        }
        return lines.isEmpty() ? List.of("待补充") : List.copyOf(lines);
    }

    private PdfFontBundle createPdfFontBundle() {
        BaseFont baseFont = null;
        for (String candidate : PDF_FONT_CANDIDATES) {
            String fontPath = candidate.contains(",")
                    ? candidate.substring(0, candidate.indexOf(','))
                    : candidate;
            if (!Files.exists(Path.of(fontPath))) {
                continue;
            }
            try {
                baseFont = BaseFont.createFont(candidate, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                break;
            } catch (DocumentException | IOException ignored) {
                // try next candidate
            }
        }

        if (baseFont == null) {
            try {
                baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            } catch (DocumentException | IOException ignored) {
                // fall through to the last-resort Latin font bundle
            }
        }

        if (baseFont == null) {
            return new PdfFontBundle(
                    new Font(Font.HELVETICA, 18, Font.BOLD),
                    new Font(Font.HELVETICA, 13, Font.BOLD),
                    new Font(Font.HELVETICA, 10, Font.NORMAL),
                    new Font(Font.HELVETICA, 11, Font.NORMAL),
                    new Font(Font.HELVETICA, 11, Font.BOLD)
            );
        }

        return new PdfFontBundle(
                new Font(baseFont, 18, Font.BOLD),
                new Font(baseFont, 13, Font.BOLD),
                new Font(baseFont, 10, Font.NORMAL),
                new Font(baseFont, 11, Font.NORMAL),
                new Font(baseFont, 11, Font.BOLD)
        );
    }

    private String workModeLabel(String value) {
        if (value == null || value.isBlank()) {
            return "待补充";
        }
        return switch (value.trim().toUpperCase()) {
            case "FULL_TIME" -> "全职";
            case "INTERNSHIP" -> "实习";
            case "PART_TIME" -> "兼职";
            case "REMOTE" -> "远程";
            case "HYBRID" -> "混合办公";
            default -> value.trim();
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String valueOrDefault(Object value) {
        if (value == null) {
            return "待补充";
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? "待补充" : text;
    }

    private record PdfFontBundle(Font title, Font heading, Font meta, Font body, Font bodyBold) {
    }

    public record ResumeExportPayload(byte[] content, String fileName, String contentType) {
    }

    private enum ResumeExportFormat {
        PDF,
        DOCX;

        private static ResumeExportFormat from(String requestedFormat) {
            if (requestedFormat == null || requestedFormat.isBlank()) {
                return PDF;
            }
            return switch (requestedFormat.trim().toLowerCase()) {
                case "pdf" -> PDF;
                case "docx", "word" -> DOCX;
                default -> throw new IllegalArgumentException("Unsupported resume export format: " + requestedFormat);
            };
        }
    }
}
