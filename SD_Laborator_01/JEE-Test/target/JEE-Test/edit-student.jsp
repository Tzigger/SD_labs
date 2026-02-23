<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head><title>Editeaza Student</title></head>
<body>
<h3>Editeaza Student</h3>

<form action="./update-student" method="post">
    <input type="hidden" name="id" value="<%= request.getParameter("id") %>"/>
    Nume: <input type="text" name="nume" value="<%= request.getParameter("nume") %>"/><br/>
    Prenume: <input type="text" name="prenume" value="<%= request.getParameter("prenume") %>"/><br/>
    Varsta: <input type="number" name="varsta" value="<%= request.getParameter("varsta") %>"/><br/><br/>
    <button type="submit">Actualizeaza</button>
</form>

<p><a href="./read-student">Inapoi la lista</a></p>
</body>
</html>
