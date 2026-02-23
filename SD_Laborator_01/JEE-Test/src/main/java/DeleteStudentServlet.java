import beans.StudentBean;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.time.Year;

public class DeleteStudentServlet extends HttpServlet {
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Doar aici modific astfel incat sa dau delete la fisier
        File file = new File("/home/tzigger/SD/date_lab/student.xml");
        if (file.exists()) {
            file.delete();
        }


        request.getRequestDispatcher("./index.jsp").forward(request, response);
    }
}