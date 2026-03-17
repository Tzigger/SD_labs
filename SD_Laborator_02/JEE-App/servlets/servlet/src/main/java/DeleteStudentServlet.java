import ejb.StudentEntity;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet pentru stergerea unui student din baza de date.
 *
 * GET  ./delete-student?id=ID
 *      → sterge studentul cu ID-ul dat (operatie DELETE via JPQL) si raporteaza rezultatul
 */
public class DeleteStudentServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html; charset=UTF-8");

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isEmpty()) {
            response.getWriter().println(
                "<html><head><meta charset='UTF-8'/></head><body>" +
                "Eroare: parametrul 'id' lipseste." +
                "<br/><a href='./fetch-student-list'>Inapoi la lista</a>" +
                "</body></html>");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(idParam);
        } catch (NumberFormatException e) {
            response.getWriter().println(
                "<html><head><meta charset='UTF-8'/></head><body>" +
                "Eroare: 'id' invalid." +
                "<br/><a href='./fetch-student-list'>Inapoi la lista</a>" +
                "</body></html>");
            return;
        }

        EntityManagerFactory factory = Persistence.createEntityManagerFactory("bazaDeDateSQLite");
        EntityManager em = factory.createEntityManager();

        try {
            EntityTransaction transaction = em.getTransaction();
            transaction.begin();

            // DELETE prin JPQL dupa ID
            int rowsDeleted = em.createQuery(
                    "DELETE FROM StudentEntity s WHERE s.id = :id")
                .setParameter("id", id)
                .executeUpdate();

            transaction.commit();

            if (rowsDeleted > 0) {
                response.getWriter().println(
                    "<html><head><meta charset='UTF-8'/></head><body>" +
                    "Studentul cu ID=" + id + " a fost sters cu succes." +
                    "<br/><br/><a href='./fetch-student-list'>Inapoi la lista studenti</a>" +
                    "<br/><a href='./'>Inapoi la meniul principal</a>" +
                    "</body></html>");
            } else {
                response.getWriter().println(
                    "<html><head><meta charset='UTF-8'/></head><body>" +
                    "Niciun student gasit cu ID=" + id + "." +
                    "<br/><br/><a href='./fetch-student-list'>Inapoi la lista studenti</a>" +
                    "</body></html>");
            }
        } finally {
            em.close();
            factory.close();
        }
    }
}
