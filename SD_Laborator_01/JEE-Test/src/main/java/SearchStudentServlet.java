import db.DatabaseHelper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchStudentServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String query = request.getParameter("query");
        List<Map<String, Object>> studenti = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            try (Connection conn = DatabaseHelper.getConnection()) {
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, nume, prenume, varsta FROM studenti WHERE nume LIKE ? OR prenume LIKE ?");
                String like = "%" + query.trim() + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                ResultSet rs = ps.executeQuery();
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
        }

        request.setAttribute("studenti", studenti);
        request.setAttribute("query", query);
        request.getRequestDispatcher("./cautare.jsp").forward(request, response);
    }
}
