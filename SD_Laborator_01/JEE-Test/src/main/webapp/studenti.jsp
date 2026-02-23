<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.List, java.util.Map" %>
<html>
<head><title>Lista Studenti</title></head>
<body>
<h3>Lista Studenti</h3>

<p><a href="./formular.jsp">Adauga student nou</a></p>
<p><a href="./search-student">Cauta student</a></p>
<p><a href="./export-json">Exporta JSON</a></p>

<%
    List<Map<String, Object>> studenti = (List<Map<String, Object>>) request.getAttribute("studenti");
    if (studenti == null || studenti.isEmpty()) {
%>
<p>Nu exista studenti in baza de date.</p>
<%
    } else {
%>
<table border="1">
    <tr>
        <th>ID</th>
        <th>Nume</th>
        <th>Prenume</th>
        <th>Varsta</th>
        <th>Actiuni</th>
    </tr>
    <%
        for (Map<String, Object> s : studenti) {
    %>
    <tr>
        <td><%= s.get("id") %></td>
        <td><%= s.get("nume") %></td>
        <td><%= s.get("prenume") %></td>
        <td><%= s.get("varsta") %></td>
        <td>
            <form action="./edit-student.jsp" method="get" style="display:inline">
                <input type="hidden" name="id" value="<%= s.get("id") %>"/>
                <input type="hidden" name="nume" value="<%= s.get("nume") %>"/>
                <input type="hidden" name="prenume" value="<%= s.get("prenume") %>"/>
                <input type="hidden" name="varsta" value="<%= s.get("varsta") %>"/>
                <button type="submit">Editeaza</button>
            </form>
            <form action="./delete-student" method="post" style="display:inline">
                <input type="hidden" name="id" value="<%= s.get("id") %>"/>
                <button type="submit">Sterge</button>
            </form>
        </td>
    </tr>
    <%
        }
    %>
</table>
<%
    }
%>
</body>
</html>
