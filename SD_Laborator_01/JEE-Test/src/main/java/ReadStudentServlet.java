import db.DatabaseHelper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReadStudentServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        List<Map<String, Object>> studenti = new ArrayList<>();

        try (Connection conn = DatabaseHelper.getConnection()) {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT id, nume, prenume, varsta FROM studenti");
            while (rs.next()) {
                Map<String, Object> s = new HashMap<>();
                s.put("id", rs.getInt("id"));
                s.put("nume", rs.getString("nume"));
                s.put("prenume", rs.getString("prenume"));
                s.put("varsta", rs.getInt("varsta"));
                studenti.add(s);
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }

        request.setAttribute("studenti", studenti);
        request.getRequestDispatcher("./studenti.jsp").forward(request, response);
    }
}
