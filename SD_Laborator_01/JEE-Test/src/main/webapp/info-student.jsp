<html xmlns:jsp="http://java.sun.com/JSP/Page">
	<head>
		<title>Informatii student</title>
		<meta charset="UTF-8" />
	</head>
	<body>
		<h3>Informatii student</h3>

		<!-- populare bean cu informatii din cererea HTTP -->
		<jsp:useBean id="studentBean" class="beans.StudentBean" scope="request" />
		<jsp:setProperty name="studentBean" property="nume" value='<%= request.getAttribute("nume") %>'/>
		<jsp:setProperty name="studentBean" property="prenume" value='<%= request.getAttribute("prenume") %>'/>
		<jsp:setProperty name="studentBean" property="varsta" value='<%= request.getAttribute("varsta") %>'/>

		<!-- folosirea bean-ului pentru afisarea informatiilor -->
		<p>Urmatoarele informatii au fost introduse:</p>
		<form action="./update-student" method="post">
            Nume: <input type="text" name="nume" value="<jsp:getProperty name="studentBean" property="nume" />" />
            <br />
            Prenume: <input type="text" name="prenume" value="<jsp:getProperty name="studentBean" property="prenume" />" />
            <br />
            Varsta: <input type="number" name="varsta" value="<jsp:getProperty name="studentBean" property="varsta" />" />
            <br />
            <br />
            <button type="submit">Actualizeaza datele</button>
        </form>

        <form action="./delete-student" method="post" style="margin-top: 20px;">
            <button type="submit" style="color: white; background-color: red;">Sterge studentul (XML)</button>
        </form>
	</body>
</html>