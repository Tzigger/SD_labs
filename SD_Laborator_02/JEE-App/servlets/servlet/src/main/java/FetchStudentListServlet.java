import ejb.StudentEntity;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

public class FetchStudentListServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // pregatire EntityManager
        EntityManagerFactory factory =   Persistence.createEntityManagerFactory("bazaDeDateSQLite");
        EntityManager em = factory.createEntityManager();

        StringBuilder responseText = new StringBuilder();
        responseText.append("<h2>Lista studenti</h2>");
        responseText.append("<table border='1'><thead><tr>" +
                "<th>ID</th><th>Nume</th><th>Prenume</th><th>Varsta</th>" +
                "<th>Actiuni</th></tr></thead>");
        responseText.append("<tbody>");

        // preluare date studenti din baza de date
        TypedQuery<StudentEntity> query = em.createQuery("select student from StudentEntity student", StudentEntity.class);
        List<StudentEntity> results = query.getResultList();
        for (StudentEntity student : results) {
            // link-uri pentru actualizare si stergere cu parametrii in URL
            String updateLink = "./update-student?id=" + student.getId() +
                    "&amp;nume=" + encodeParam(student.getNume()) +
                    "&amp;prenume=" + encodeParam(student.getPrenume()) +
                    "&amp;varsta=" + student.getVarsta();
            String deleteLink = "./delete-student?id=" + student.getId();

            // se creeaza cate un rand de tabel HTML pentru fiecare student gasit
            responseText.append("<tr><td>" + student.getId() + "</td><td>" +
                    student.getNume() + "</td><td>" +
                    student.getPrenume() + "</td><td>" +
                    student.getVarsta() + "</td><td>" +
                    "<a href='" + updateLink + "'>Actualizeaza</a> | " +
                    "<a href='" + deleteLink + "' " +
                    "onclick=\"return confirm('Stergeti studentul " +
                    student.getNume() + " " +
                    student.getPrenume() + "?')\">Sterge</a>" +
                    "</td></tr>");
        }

        responseText.append("</tbody></table><br /><br /><a href='./'>Inapoi la meniul principal</a>");

        // inchidere EntityManager
        em.close();
        factory.close();

        // trimitere raspuns la client
        response.setContentType("text/html; charset=UTF-8");
        response.getWriter().print(responseText.toString());
    }

    private String encodeParam(String value) {
        if (value == null) return "";
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
}
