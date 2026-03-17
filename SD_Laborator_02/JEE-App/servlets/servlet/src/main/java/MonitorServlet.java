import ejb.StudentEntity;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Servlet de monitorizare a bazei de date.
 *
 * Starea (lista de alarme, scheduler, parametri) este salvata in ServletContext
 * pentru a supravietui reincarcarii servlet-ului de catre GlassFish.
 *
 * Campuri monitorizate:
 *   - varsta : intervalul valid [minVarsta, maxVarsta]  (implicit [18, 30])
 *   - id     : intervalul valid [minId, maxId]          (implicit [1, 1000])
 *
 * URL-uri:
 *   GET ./monitor                        – pagina status + formular configurare
 *   GET ./monitor?action=start&...       – pornire monitorizare cu parametrii dati
 *   GET ./monitor?action=stop            – oprire monitorizare
 *   GET ./monitor?action=alarms          – pagina de alarmare
 *   GET ./monitor?action=clear           – sterge lista de alarme
 */
public class MonitorServlet extends HttpServlet {

    // Chei folosite in ServletContext — starea supravietuieste redeployului
    private static final String CTX_ALARMS    = "monitor.alarms";
    private static final String CTX_SCHEDULER = "monitor.scheduler";
    private static final String CTX_RUNNING   = "monitor.running";
    private static final String CTX_MIN_V     = "monitor.minVarsta";
    private static final String CTX_MAX_V     = "monitor.maxVarsta";
    private static final String CTX_MIN_ID    = "monitor.minId";
    private static final String CTX_MAX_ID    = "monitor.maxId";
    private static final String CTX_INTERVAL  = "monitor.intervalSeconds";

    // ---------- structura unei alarme ----------
    public static class Alarm {
        public final long   timestamp;
        public final String campNume;
        public final Number valoare;
        public final Number limitaMin;
        public final Number limitaMax;
        public final int    studentId;
        public final String studentNume;
        public final String studentPrenume;

        public Alarm(long timestamp, String campNume, Number valoare,
                     Number limitaMin, Number limitaMax,
                     int studentId, String studentNume, String studentPrenume) {
            this.timestamp      = timestamp;
            this.campNume       = campNume;
            this.valoare        = valoare;
            this.limitaMin      = limitaMin;
            this.limitaMax      = limitaMax;
            this.studentId      = studentId;
            this.studentNume    = studentNume;
            this.studentPrenume = studentPrenume;
        }
    }

    // ---------- init ----------
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext ctx = config.getServletContext();
        // Initializeaza lista de alarme doar daca nu exista deja in context
        if (ctx.getAttribute(CTX_ALARMS) == null) {
            ctx.setAttribute(CTX_ALARMS,   new ArrayList<Alarm>());
            ctx.setAttribute(CTX_RUNNING,  false);
            ctx.setAttribute(CTX_MIN_V,    18);
            ctx.setAttribute(CTX_MAX_V,    30);
            ctx.setAttribute(CTX_MIN_ID,   1);
            ctx.setAttribute(CTX_MAX_ID,   1000);
            ctx.setAttribute(CTX_INTERVAL, 10);
        }
    }

    // ---------- destroy ----------
    @Override
    public void destroy() {
        stopMonitor(getServletContext());
        super.destroy();
    }

    // ---------- GET ----------
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html; charset=UTF-8");
        String action = request.getParameter("action");
        if      ("start" .equals(action)) handleStart (request, response);
        else if ("stop"  .equals(action)) handleStop  (request, response);
        else if ("alarms".equals(action)) handleAlarms(request, response);
        else if ("clear" .equals(action)) handleClear (request, response);
        else                              handleStatus (request, response);
    }

    @SuppressWarnings("unchecked")
    private void handleStart(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        ServletContext ctx = getServletContext();

        int minVarsta       = parseIntParam(request, "minVarsta",       18);
        int maxVarsta       = parseIntParam(request, "maxVarsta",       30);
        int minId           = parseIntParam(request, "minId",            1);
        int maxId           = parseIntParam(request, "maxId",         1000);
        int intervalSeconds = parseIntParam(request, "intervalSeconds", 10);

        ctx.setAttribute(CTX_MIN_V,    minVarsta);
        ctx.setAttribute(CTX_MAX_V,    maxVarsta);
        ctx.setAttribute(CTX_MIN_ID,   minId);
        ctx.setAttribute(CTX_MAX_ID,   maxId);
        ctx.setAttribute(CTX_INTERVAL, intervalSeconds);

        ((List<Alarm>) ctx.getAttribute(CTX_ALARMS)).clear();
        stopMonitor(ctx);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "StudentMonitorThread");
            t.setDaemon(true);
            return t;
        });

        final int fMinV  = minVarsta, fMaxV  = maxVarsta;
        final int fMinId = minId,     fMaxId = maxId;

        scheduler.scheduleAtFixedRate(
            () -> checkDatabase(ctx, fMinV, fMaxV, fMinId, fMaxId),
            0, intervalSeconds, TimeUnit.SECONDS
        );

        ctx.setAttribute(CTX_SCHEDULER, scheduler);
        ctx.setAttribute(CTX_RUNNING,   true);

        PrintWriter out = response.getWriter();
        out.println("<html><head><meta charset='UTF-8'/></head><body>");
        out.println("<h3>Monitorizare pornita</h3><ul>");
        out.println("<li>Interval varsta: [" + minVarsta + ", " + maxVarsta + "]</li>");
        out.println("<li>Interval ID: [" + minId + ", " + maxId + "]</li>");
        out.println("<li>Frecventa: " + intervalSeconds + " secunde</li>");
        out.println("</ul>");
        out.println("<a href='./monitor?action=alarms'>Vezi alarme</a> | ");
        out.println("<a href='./monitor?action=stop'>Opreste</a> | ");
        out.println("<a href='./'>Inapoi</a></body></html>");
    }

    private void handleStop(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        stopMonitor(getServletContext());
        PrintWriter out = response.getWriter();
        out.println("<html><head><meta charset='UTF-8'/></head><body>");
        out.println("<h3>Monitorizare oprita.</h3>");
        out.println("<a href='./monitor'>Configurare</a> | <a href='./'>Inapoi</a>");
        out.println("</body></html>");
    }

    @SuppressWarnings("unchecked")
    private void handleClear(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        List<Alarm> alarms = (List<Alarm>) getServletContext().getAttribute(CTX_ALARMS);
        if (alarms != null) alarms.clear();
        response.sendRedirect("./monitor?action=alarms");
    }

    @SuppressWarnings("unchecked")
    private void handleAlarms(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        ServletContext ctx  = getServletContext();
        List<Alarm> alarms  = (List<Alarm>) ctx.getAttribute(CTX_ALARMS);
        boolean running     = Boolean.TRUE.equals(ctx.getAttribute(CTX_RUNNING));
        int minVarsta       = getInt(ctx, CTX_MIN_V,    18);
        int maxVarsta       = getInt(ctx, CTX_MAX_V,    30);
        int minId           = getInt(ctx, CTX_MIN_ID,    1);
        int maxId           = getInt(ctx, CTX_MAX_ID, 1000);

        PrintWriter out = response.getWriter();
        out.println("<html><head><meta charset='UTF-8'/></head><body>");
        out.println("<h2>Alarme detectate</h2>");
        out.println("<p>Monitor: " + (running ? "ACTIV" : "OPRIT") +
                " | Interval varsta: [" + minVarsta + ", " + maxVarsta +
                "] | Interval ID: [" + minId + ", " + maxId + "]</p>");

        int count = (alarms == null) ? 0 : alarms.size();

        if (count == 0) {
            out.println("<p>Nu exista alarme.</p>");
        } else {
            out.println("<table border='1'><thead><tr>" +
                        "<th>#</th><th>Moment</th><th>ID</th><th>Nume</th><th>Prenume</th>" +
                        "<th>Camp</th><th>Valoare</th><th>Min</th><th>Max</th>" +
                        "</tr></thead><tbody>");
            synchronized (alarms) {
                int nr = 1;
                for (Alarm a : alarms) {
                    out.println("<tr><td>" + nr++ + "</td>" +
                            "<td>" + new java.util.Date(a.timestamp) + "</td>" +
                            "<td>" + a.studentId + "</td>" +
                            "<td>" + a.studentNume + "</td>" +
                            "<td>" + a.studentPrenume + "</td>" +
                            "<td>" + a.campNume + "</td>" +
                            "<td>" + a.valoare + "</td>" +
                            "<td>" + a.limitaMin + "</td>" +
                            "<td>" + a.limitaMax + "</td></tr>");
                }
            }
            out.println("</tbody></table>");
        }
        out.println("<br/><a href='./monitor?action=alarms'>Reincarca</a> | ");
        out.println("<a href='./monitor?action=clear'>Sterge alarme</a> | ");
        out.println("<a href='./monitor'>Configurare</a> | ");
        out.println("<a href='./'>Inapoi</a></body></html>");
    }

    private void handleStatus(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        ServletContext ctx  = getServletContext();
        boolean running     = Boolean.TRUE.equals(ctx.getAttribute(CTX_RUNNING));
        int minVarsta       = getInt(ctx, CTX_MIN_V,    18);
        int maxVarsta       = getInt(ctx, CTX_MAX_V,    30);
        int minId           = getInt(ctx, CTX_MIN_ID,    1);
        int maxId           = getInt(ctx, CTX_MAX_ID, 1000);
        int intervalSeconds = getInt(ctx, CTX_INTERVAL,  10);
        int alarmCount      = ctx.getAttribute(CTX_ALARMS) == null ? 0 :
                              ((List<?>) ctx.getAttribute(CTX_ALARMS)).size();

        PrintWriter out = response.getWriter();
        out.println("<html><head><meta charset='UTF-8'/></head><body>");
        out.println("<h3>Monitorizare baza de date</h3>");
        out.println("<p>Stare: " + (running ? "ACTIV" : "OPRIT") + "</p>");
        out.println("<h4>Configurare si pornire</h4>");
        out.println("<form method='get' action='./monitor'>");
        out.println("<input type='hidden' name='action' value='start'/>");
        out.println("Min varsta: <input type='number' name='minVarsta' value='" + minVarsta + "'/><br/>");
        out.println("Max varsta: <input type='number' name='maxVarsta' value='" + maxVarsta + "'/><br/>");
        out.println("Min ID: <input type='number' name='minId' value='" + minId + "'/><br/>");
        out.println("Max ID: <input type='number' name='maxId' value='" + maxId + "'/><br/>");
        out.println("Interval (secunde): <input type='number' name='intervalSeconds' value='" + intervalSeconds + "'/><br/><br/>");
        out.println("<button type='submit'>Porneste monitorizarea</button>");
        out.println("</form><br/>");
        out.println("<a href='./monitor?action=alarms'>Vezi alarme (" + alarmCount + ")</a> | ");
        out.println("<a href='./monitor?action=stop'>Opreste</a> | ");
        out.println("<a href='./'>Inapoi</a></body></html>");
    }

    @SuppressWarnings("unchecked")
    private void checkDatabase(ServletContext ctx,
                               int minVarsta, int maxVarsta,
                               int minId,     int maxId) {
        EntityManagerFactory factory = null;
        EntityManager em = null;
        try {
            factory = Persistence.createEntityManagerFactory("bazaDeDateSQLite");
            em = factory.createEntityManager();

            List<StudentEntity> students = em
                .createQuery("SELECT s FROM StudentEntity s", StudentEntity.class)
                .getResultList();

            long now = System.currentTimeMillis();
            List<Alarm> alarms = (List<Alarm>) ctx.getAttribute(CTX_ALARMS);
            if (alarms == null) return;

            synchronized (alarms) {
                for (StudentEntity s : students) {
                    if (s.getVarsta() < minVarsta || s.getVarsta() > maxVarsta) {
                        alarms.add(new Alarm(now, "varsta", s.getVarsta(),
                                minVarsta, maxVarsta,
                                s.getId(), s.getNume(), s.getPrenume()));
                        System.err.println("[MONITOR ALARMA] " + s.getNume() + " " + s.getPrenume() +
                                " varsta=" + s.getVarsta() +
                                " in afara [" + minVarsta + "," + maxVarsta + "]");
                    }
                    if (s.getId() < minId || s.getId() > maxId) {
                        alarms.add(new Alarm(now, "id", s.getId(),
                                minId, maxId,
                                s.getId(), s.getNume(), s.getPrenume()));
                        System.err.println("[MONITOR ALARMA] " + s.getNume() + " " + s.getPrenume() +
                                " id=" + s.getId() +
                                " in afara [" + minId + "," + maxId + "]");
                    }
                }
            }
            System.out.println("[MONITOR] Verificare OK: " + students.size() + " studenti, " +
                    ((List<?>) ctx.getAttribute(CTX_ALARMS)).size() + " alarme totale.");
        } catch (Exception ex) {
            System.err.println("[MONITOR] Eroare: " + ex.getMessage());
        } finally {
            if (em      != null) try { em.close();      } catch (Exception ignored) {}
            if (factory != null) try { factory.close(); } catch (Exception ignored) {}
        }
    }

    
    //HELPERS
    private static void stopMonitor(ServletContext ctx) {
        Object s = ctx.getAttribute(CTX_SCHEDULER);
        if (s instanceof ScheduledExecutorService) {
            ScheduledExecutorService sched = (ScheduledExecutorService) s;
            if (!sched.isShutdown()) sched.shutdownNow();
        }
        ctx.setAttribute(CTX_SCHEDULER, null);
        ctx.setAttribute(CTX_RUNNING,   false);
    }

    private int parseIntParam(HttpServletRequest req, String name, int def) {
        String v = req.getParameter(name);
        if (v == null || v.isEmpty()) return def;
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return def; }
    }

    private int getInt(ServletContext ctx, String key, int def) {
        Object v = ctx.getAttribute(key);
        return (v instanceof Integer) ? (Integer) v : def;
    }

}
