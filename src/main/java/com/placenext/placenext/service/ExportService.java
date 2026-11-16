package com.placenext.placenext.service;

import com.placenext.placenext.model.Application;
import com.placenext.placenext.model.OfferStatus;
import com.placenext.placenext.model.StudentProfile;
import com.placenext.placenext.repository.ApplicationRepository;
import com.placenext.placenext.repository.StudentProfileRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExportService {

    private final StudentProfileRepository studentProfileRepository;
    private final ApplicationRepository applicationRepository;
    private final ReportService reportService;

    public ExportService(StudentProfileRepository studentProfileRepository,
                         ApplicationRepository applicationRepository,
                         ReportService reportService) {
        this.studentProfileRepository = studentProfileRepository;
        this.applicationRepository = applicationRepository;
        this.reportService = reportService;
    }

    /** All students, with their placement details if they have accepted an offer. */
    public String studentsCsv() {
        Map<Long, Application> placedByStudent = new HashMap<>();
        for (Application a : applicationRepository.findByOfferStatusOrderByRespondedAtDesc(OfferStatus.ACCEPTED)) {
            placedByStudent.put(a.getStudent().getId(), a);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Name,Email,Roll No,Branch,Batch,CGPA,Backlogs,Phone,Placed,Company,Role,Package\n");
        for (StudentProfile s : studentProfileRepository.findAll()) {
            Application placed = placedByStudent.get(s.getId());
            sb.append(csv(s.getUser().getFullName())).append(',')
                    .append(csv(s.getUser().getEmail())).append(',')
                    .append(csv(s.getRollNo())).append(',')
                    .append(csv(s.getBranch() == null ? null : s.getBranch().name())).append(',')
                    .append(s.getBatch()).append(',')
                    .append(s.getCgpa()).append(',')
                    .append(s.getActiveBacklogs()).append(',')
                    .append(csv(s.getPhone())).append(',')
                    .append(placed != null ? "Yes" : "No").append(',')
                    .append(csv(placed != null ? placed.getDrive().getCompanyName() : null)).append(',')
                    .append(csv(placed != null ? placed.getDrive().getTitle() : null)).append(',')
                    .append(csv(placed != null ? placed.getDrive().getCompensation() : null)).append('\n');
        }
        return sb.toString();
    }

    /** One row per accepted offer. */
    public String placementsCsv() {
        List<Application> placed = applicationRepository
                .findByOfferStatusOrderByRespondedAtDesc(OfferStatus.ACCEPTED);

        StringBuilder sb = new StringBuilder();
        sb.append("Name,Email,Roll No,Branch,CGPA,Company,Role,Package,Accepted On\n");
        for (Application a : placed) {
            StudentProfile s = a.getStudent();
            sb.append(csv(s.getUser().getFullName())).append(',')
                    .append(csv(s.getUser().getEmail())).append(',')
                    .append(csv(s.getRollNo())).append(',')
                    .append(csv(s.getBranch() == null ? null : s.getBranch().name())).append(',')
                    .append(s.getCgpa()).append(',')
                    .append(csv(a.getDrive().getCompanyName())).append(',')
                    .append(csv(a.getDrive().getTitle())).append(',')
                    .append(csv(a.getDrive().getCompensation())).append(',')
                    .append(a.getRespondedAt() == null ? "" : a.getRespondedAt().toLocalDate()).append('\n');
        }
        return sb.toString();
    }

    /** Summary, branch-wise and company-wise sections in one file. */
    public String reportCsv() {
        ReportService.Report r = reportService.build();

        StringBuilder sb = new StringBuilder();
        sb.append("Summary\n");
        sb.append("Students,").append(r.getTotalStudents()).append('\n');
        sb.append("Placed,").append(r.getTotalPlaced()).append('\n');
        sb.append("Placement rate %,").append(r.getOverallPercent()).append('\n');
        sb.append("Full-time offers,").append(r.getFullTimeOffers()).append('\n');
        sb.append("Internship offers,").append(r.getInternshipOffers()).append('\n');
        sb.append("Highest package (LPA),").append(r.getHighestLpa() == null ? "" : r.getHighestLpa()).append('\n');
        sb.append("Average package (LPA),").append(r.getAverageLpa() == null ? "" : r.getAverageLpa()).append('\n');
        sb.append("Lowest package (LPA),").append(r.getLowestLpa() == null ? "" : r.getLowestLpa()).append('\n');

        sb.append("\nBranch-wise\n");
        sb.append("Branch,Students,Placed,Placement %\n");
        for (ReportService.BranchRow b : r.getBranches()) {
            sb.append(csv(b.getBranch())).append(',')
                    .append(b.getTotal()).append(',')
                    .append(b.getPlaced()).append(',')
                    .append(b.getPercent()).append('\n');
        }

        sb.append("\nCompany-wise\n");
        sb.append("Company,Students hired,Average package (LPA)\n");
        for (ReportService.CompanyRow c : r.getCompanies()) {
            sb.append(csv(c.getCompany())).append(',')
                    .append(c.getHires()).append(',')
                    .append(c.getAverageLpa() == null ? "" : c.getAverageLpa()).append('\n');
        }
        return sb.toString();
    }

    private String csv(String value) {
        if (value == null) return "";
        String v = value;
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }
}