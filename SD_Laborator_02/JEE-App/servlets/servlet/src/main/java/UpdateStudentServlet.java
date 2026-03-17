import ejb.StudentEntity;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Servlet pentru actualizarea datelor unui student.
 *
 * GET  ./update-student?id=ID&nume=NUME&prenume=PRENUME&varsta=VARSTA
 *      → afiseaza formularul pre-completat cu datele curente ale studentului
 *
 * POST ./update-student  (cu campurile id, nume, prenume, varsta)
 *      → executa UPDATE prin JPQL si raporteaza rezultatul
 */
public class UpdateStudentServlet extends HttpServlet {

    /** Afiseaza formularul de editare pre-completat cu datele existente. */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String id      = request.getParameter("id");
        String nume    = request.getParameter("nume");
        String prenume = request.getParameter("prenume");
        String varsta  = request.getParameter("varsta");

        response.setContentType("text/html; charset=UTF-8");
        response.getWriter().println(
            "<html><head><title>Actualizare student</title>" +
            "<meta charset='UTF-8'/></head><body>" +
            "<h3>Actualizare student</h3>" +
            "<form action='./update-student' method='post'>" +
            "<input type='hidden' name='id' value='" + id + "' />" +
            "Nume: <input type='text' name='nume' value='" + nume + "' /><br/>" +
            "Prenume: <input type='text' name='prenume' value='" + prenume + "' /><br/>" +
            "Varsta: <input type='number' name='varsta' value='" + varsta + "' /><br/><br/>" +
            "<button type='submit'>Salveaza modificarile</button>" +
            "</form>" +
            "<br/><a href='./fetch-student-list'>Inapoi la lista studenti</a>" +
            "</body></html>"
        );
    }

    /** Executa operatia de UPDATE folosind JPQL dupa ID-ul primit. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html; charset=UTF-8");

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isEmpty()) {
            response.getWriter().println("Eroare: parametrul 'id' lipseste." +
                    "<br/><a href='./fetch-student-list'>Inapoi la lista</a>");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(idParam);
        } catch (NumberFormatException e) {
            response.getWriter().println("Eroare: 'id' invalid." +
                    "<br/><a href='./fetch-student-list'>Inapoi la lista</a>");
            return;
        }

        String numeNou    = request.getParameter("nume");
        String prenumeNou = request.getParameter("prenume");
        int varstaNoua;
        try {
            varstaNoua = Integer.parseInt(request.getParameter("varsta"));
        } catch (NumberFormatException e) {
            response.getWriter().println("Eroare: 'varsta' invalida." +
                    "<br/><a href='./fetch-student-list'>Inapoi la lista</a>");
            return;
        }

        EntityManagerFactory factory = Persistence.createEntityManagerFactory("bazaDeDateSQLite");
        EntityManager em = factory.createEntityManager();

        try {
            EntityTransaction transaction = em.getTransaction();
            transaction.begin();

            // UPDATE prin JPQL dupa ID
            int rowsUpdated = em.createQuery(
                    "UPDATE StudentEntity s SET s.nume = :numeNou, " +
                    "s.prenume = :prenumeNou, s.varsta = :varstaNoua " +
                    "WHERE s.id = :id")
                .setParameter("numeNou",    numeNou)
                .setParameter("prenumeNou", prenumeNou)
                .setParameter("varstaNoua", varstaNoua)
                .setParameter("id",         id)
                .executeUpdate();

            transaction.commit();

            if (rowsUpdated > 0) {
                response.getWriter().println(
                    "<html><head><meta charset='UTF-8'/></head><body>" +
                    "Studentul cu ID=" + id + " a fost actualizat cu succes." +
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
