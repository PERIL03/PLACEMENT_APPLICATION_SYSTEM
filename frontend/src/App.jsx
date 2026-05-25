import { useEffect, useState } from "react";

const API_BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080/api/v1";
const AUTH_STORAGE_KEY = "placementAuth";

function App() {
  const [auth, setAuth] = useState(() => {
    try {
      const saved = localStorage.getItem(AUTH_STORAGE_KEY);
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });
  const [loginForm, setLoginForm] = useState({ username: "", password: "" });
  const [loginError, setLoginError] = useState("");
  const [loggingIn, setLoggingIn] = useState(false);

  const [health, setHealth] = useState(null);
  const [students, setStudents] = useState([]);
  const [drives, setDrives] = useState([]);
  const [applications, setApplications] = useState([]);
  const [selectedStudentId, setSelectedStudentId] = useState("");
  const [applyingDriveId, setApplyingDriveId] = useState(null);
  const [actionMessage, setActionMessage] = useState("");
  const [actionError, setActionError] = useState("");
  const [shortlistingDriveId, setShortlistingDriveId] = useState(null);
  const [statusUpdatingAppId, setStatusUpdatingAppId] = useState(null);
  const [schedulingAppId, setSchedulingAppId] = useState(null);
  const [loadingHistoryAppId, setLoadingHistoryAppId] = useState(null);
  const [interviewForms, setInterviewForms] = useState({});
  const [historyByApplication, setHistoryByApplication] = useState({});
  const [loading, setLoading] = useState(true);

  const canManageApplications = auth?.role === "PLACEMENT_OFFICER" || auth?.role === "RECRUITER";

  const authFetch = async (path, options = {}) => {
    if (!auth?.token) {
      throw new Error("Please login first");
    }

    const response = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${auth.token}`,
        ...(options.headers || {}),
      },
    });

    if (response.status === 401) {
      localStorage.removeItem(AUTH_STORAGE_KEY);
      setAuth(null);
      throw new Error("Session expired. Please login again.");
    }

    return response;
  };

  useEffect(() => {
    const loadInitialData = async () => {
      if (!auth?.token) {
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        const [healthRes, studentsRes, drivesRes] = await Promise.all([
          fetch(`${API_BASE}/health`),
          authFetch("/students"),
          authFetch("/drives"),
        ]);

        const healthData = await healthRes.json();
        const studentsData = await studentsRes.json();
        const drivesData = await drivesRes.json();

        setHealth(healthData);
        setStudents(studentsData);
        setDrives(drivesData);
        if (studentsData.length > 0) {
          setSelectedStudentId(String(studentsData[0].id));
        }
      } catch (error) {
        setActionError(error.message || "Failed to load data");
      } finally {
        setLoading(false);
      }
    };

    loadInitialData();
  }, [auth]);

  const loadApplications = async () => {
    if (!selectedStudentId || !auth?.token) {
      setApplications([]);
      return;
    }

    try {
      const response = await authFetch(`/applications/students/${selectedStudentId}`);
      const data = await response.json();
      setApplications(data);
    } catch (error) {
      setActionError(error.message || "Failed to load applications");
    }
  };

  useEffect(() => {
    loadApplications();
  }, [selectedStudentId, auth]);

  const handleLogin = async (event) => {
    event.preventDefault();
    setLoginError("");
    setLoggingIn(true);

    try {
      const response = await fetch(`${API_BASE}/auth/login`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(loginForm),
      });

      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.error || "Login failed");
      }

      localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(data));
      setAuth(data);
      setLoginForm({ username: "", password: "" });
    } catch (error) {
      setLoginError(error.message || "Login failed");
    } finally {
      setLoggingIn(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem(AUTH_STORAGE_KEY);
    setAuth(null);
    setStudents([]);
    setDrives([]);
    setApplications([]);
    setSelectedStudentId("");
      setInterviewForms({});
      setHistoryByApplication({});
    setActionError("");
    setActionMessage("");
  };

  const handleApply = async (driveId) => {
    if (!selectedStudentId) {
      return;
    }

    if (auth?.role !== "STUDENT") {
      setActionError("Only users with STUDENT role can apply to drives.");
      return;
    }

    setActionMessage("");
    setActionError("");
    setApplyingDriveId(driveId);

    try {
      const response = await authFetch("/applications", {
        method: "POST",
        body: JSON.stringify({
          driveId,
        }),
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.error || "Application failed");
      }

      setActionMessage("Application submitted successfully.");
      await loadApplications();
    } catch (error) {
      setActionError(error.message || "Application failed");
    } finally {
      setApplyingDriveId(null);
    }
  };

  const handleAutoShortlist = async (driveId) => {
    setActionMessage("");
    setActionError("");
    setShortlistingDriveId(driveId);

    try {
      const response = await authFetch(`/applications/drives/${driveId}/auto-shortlist`, {
        method: "POST",
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.error || "Auto-shortlisting failed");
      }

      const shortlisted = await response.json();
      setActionMessage(`Auto-shortlisting completed. Processed ${shortlisted.length} applications.`);

      if (selectedStudentId) {
        await loadApplications();
      }
    } catch (error) {
      setActionError(error.message || "Auto-shortlisting failed");
    } finally {
      setShortlistingDriveId(null);
    }
  };

  const handleStatusUpdate = async (applicationId, status, remarks) => {
    setActionMessage("");
    setActionError("");
    setStatusUpdatingAppId(applicationId);

    try {
      const response = await authFetch(`/applications/${applicationId}/status`, {
        method: "POST",
        body: JSON.stringify({ status, remarks }),
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.error || "Status update failed");
      }

      setActionMessage(`Application moved to ${status}.`);
      await loadApplications();
      if (historyByApplication[applicationId]) {
        await handleHistoryToggle(applicationId, true);
      }
    } catch (error) {
      setActionError(error.message || "Status update failed");
    } finally {
      setStatusUpdatingAppId(null);
    }
  };

  const handleInterviewField = (applicationId, field, value) => {
    setInterviewForms((prev) => ({
      ...prev,
      [applicationId]: {
        scheduledAt: prev[applicationId]?.scheduledAt || "",
        mode: prev[applicationId]?.mode || "ONLINE",
        meetingLink: prev[applicationId]?.meetingLink || "",
        [field]: value,
      },
    }));
  };

  const handleScheduleInterview = async (applicationId) => {
    const form = interviewForms[applicationId] || { scheduledAt: "", mode: "ONLINE", meetingLink: "" };
    if (!form.scheduledAt) {
      setActionError("Please choose interview date and time.");
      return;
    }

    setActionMessage("");
    setActionError("");
    setSchedulingAppId(applicationId);

    try {
      const response = await authFetch(`/applications/${applicationId}/interview-slot`, {
        method: "POST",
        body: JSON.stringify({
          scheduledAt: form.scheduledAt,
          mode: form.mode,
          meetingLink: form.meetingLink,
        }),
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.error || "Interview scheduling failed");
      }

      setActionMessage("Interview slot scheduled successfully.");
      await loadApplications();
      await handleHistoryToggle(applicationId, true);
    } catch (error) {
      setActionError(error.message || "Interview scheduling failed");
    } finally {
      setSchedulingAppId(null);
    }
  };

  const handleHistoryToggle = async (applicationId, forceLoad = false) => {
    if (!forceLoad && historyByApplication[applicationId]) {
      setHistoryByApplication((prev) => {
        const next = { ...prev };
        delete next[applicationId];
        return next;
      });
      return;
    }

    setLoadingHistoryAppId(applicationId);
    try {
      const response = await authFetch(`/applications/${applicationId}/history`);
      const data = await response.json();
      setHistoryByApplication((prev) => ({
        ...prev,
        [applicationId]: data,
      }));
    } catch (error) {
      setActionError(error.message || "Failed to load status history");
    } finally {
      setLoadingHistoryAppId(null);
    }
  };

  if (!auth?.token) {
    return (
      <main className="page">
        <section className="hero">
          <p className="tag">College Placement Management</p>
          <h1>Login Portal</h1>
          <p className="subtitle">Sign in to access role-based placement workflows.</p>
        </section>

        <section className="panel auth-panel">
          <h2>Login</h2>
          <form className="auth-form" onSubmit={handleLogin}>
            <label htmlFor="username">Username</label>
            <input
              id="username"
              type="text"
              value={loginForm.username}
              onChange={(event) => setLoginForm((prev) => ({ ...prev, username: event.target.value }))}
              required
            />

            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              value={loginForm.password}
              onChange={(event) => setLoginForm((prev) => ({ ...prev, password: event.target.value }))}
              required
            />

            <button className="apply-btn" type="submit" disabled={loggingIn}>
              {loggingIn ? "Signing in..." : "Sign In"}
            </button>
          </form>

          {loginError && <p className="error-text">{loginError}</p>}

          <div className="demo-accounts">
            <p>Demo accounts:</p>
            <p>Student: student1 / student123</p>
            <p>Student: student2 / student234</p>
            <p>Placement Officer: officer1 / officer123</p>
            <p>Recruiter: recruiter1 / recruiter123</p>
          </div>
        </section>
      </main>
    );
  }

  return (
    <main className="page">
      <section className="hero">
        <p className="tag">College Placement Management</p>
        <h1>Placement Control Room</h1>
        <p className="subtitle">
          Starter dashboard to track students, company drives, and shortlisting workflows.
        </p>
        <p className="session-info">
          Signed in as <strong>{auth.username}</strong> ({auth.role})
        </p>
        <button className="apply-btn" type="button" onClick={handleLogout}>
          Logout
        </button>
      </section>

      <section className="panel">
        <h2>Backend Status</h2>
        <p>
          {loading
            ? "Checking backend..."
            : health
            ? `${health.service} is ${health.status}`
            : "Backend unavailable"}
        </p>
      </section>

      <section className="panel">
        <h2>Student Snapshot</h2>
        <label className="student-picker" htmlFor="student-select">
          Active student
          <select
            id="student-select"
            value={selectedStudentId}
            onChange={(event) => setSelectedStudentId(event.target.value)}
          >
            {students.map((student) => (
              <option key={student.id} value={student.id}>
                {student.fullName}
              </option>
            ))}
          </select>
        </label>
        {loading ? (
          <p>Loading students...</p>
        ) : students.length === 0 ? (
          <p>No students available.</p>
        ) : (
          <div className="student-grid">
            {students.map((student) => (
              <article className="student-card" key={student.id}>
                <h3>{student.fullName}</h3>
                <p>{student.branch}</p>
                <p>Graduation: {student.graduationYear}</p>
                <p>CGPA: {student.cgpa}</p>
              </article>
            ))}
          </div>
        )}
      </section>

      <section className="panel">
        <h2>Open Drives</h2>
        {drives.length === 0 ? (
          <p>No drives available.</p>
        ) : (
          <div className="student-grid">
            {drives.map((drive) => (
              <article className="student-card" key={drive.id}>
                <h3>{drive.roleTitle}</h3>
                <p>Company: {drive.companyName}</p>
                <p>Branch: {drive.eligibleBranch}</p>
                <p>Minimum CGPA: {drive.minCgpa}</p>
                <p>Package: {drive.packageLpa} LPA</p>
                <p>Deadline: {drive.applicationDeadline}</p>
                <button
                  className="apply-btn"
                  type="button"
                  onClick={() => handleApply(drive.id)}
                  disabled={applyingDriveId === drive.id || !selectedStudentId || auth.role !== "STUDENT"}
                >
                  {applyingDriveId === drive.id ? "Applying..." : "Apply"}
                </button>

                {auth.role === "PLACEMENT_OFFICER" && (
                  <button
                    className="apply-btn secondary-btn"
                    type="button"
                    onClick={() => handleAutoShortlist(drive.id)}
                    disabled={shortlistingDriveId === drive.id}
                  >
                    {shortlistingDriveId === drive.id ? "Shortlisting..." : "Auto-Shortlist"}
                  </button>
                )}
              </article>
            ))}
          </div>
        )}

        {actionMessage && <p className="success-text">{actionMessage}</p>}
        {actionError && <p className="error-text">{actionError}</p>}
      </section>

      <section className="panel">
        <h2>Application History</h2>
        {applications.length === 0 ? (
          <p>No applications yet for selected student.</p>
        ) : (
          <div className="application-list">
            {applications.map((app) => (
              <article className="application-item" key={app.id}>
                <h3>
                  {app.companyName} - {app.roleTitle}
                </h3>
                <p>Status: {app.status}</p>
                <p>Applied At: {new Date(app.appliedAt).toLocaleString()}</p>

                {app.interviewAt && (
                  <>
                    <p>Interview At: {new Date(app.interviewAt).toLocaleString()}</p>
                    <p>Mode: {app.interviewMode || "-"}</p>
                    {app.interviewLink && <p>Meeting Link: {app.interviewLink}</p>}
                  </>
                )}

                {canManageApplications && (
                  <div className="action-row">
                    <button
                      className="apply-btn mini-btn"
                      type="button"
                      onClick={() => handleStatusUpdate(app.id, "SHORTLISTED", "Marked by recruiter/officer")}
                      disabled={statusUpdatingAppId === app.id}
                    >
                      Shortlist
                    </button>
                    <button
                      className="apply-btn mini-btn secondary-btn"
                      type="button"
                      onClick={() => handleStatusUpdate(app.id, "REJECTED", "Rejected after review")}
                      disabled={statusUpdatingAppId === app.id}
                    >
                      Reject
                    </button>
                    <button
                      className="apply-btn mini-btn"
                      type="button"
                      onClick={() => handleStatusUpdate(app.id, "OFFERED", "Offer released")}
                      disabled={statusUpdatingAppId === app.id}
                    >
                      Offer
                    </button>
                  </div>
                )}

                {canManageApplications && (
                  <div className="interview-form">
                    <input
                      type="datetime-local"
                      value={interviewForms[app.id]?.scheduledAt || ""}
                      onChange={(event) => handleInterviewField(app.id, "scheduledAt", event.target.value)}
                    />
                    <select
                      value={interviewForms[app.id]?.mode || "ONLINE"}
                      onChange={(event) => handleInterviewField(app.id, "mode", event.target.value)}
                    >
                      <option value="ONLINE">ONLINE</option>
                      <option value="OFFLINE">OFFLINE</option>
                    </select>
                    <input
                      type="text"
                      placeholder="Meeting link or venue"
                      value={interviewForms[app.id]?.meetingLink || ""}
                      onChange={(event) => handleInterviewField(app.id, "meetingLink", event.target.value)}
                    />
                    <button
                      className="apply-btn mini-btn secondary-btn"
                      type="button"
                      onClick={() => handleScheduleInterview(app.id)}
                      disabled={schedulingAppId === app.id}
                    >
                      {schedulingAppId === app.id ? "Scheduling..." : "Schedule Interview"}
                    </button>
                  </div>
                )}

                <button
                  className="apply-btn mini-btn history-btn"
                  type="button"
                  onClick={() => handleHistoryToggle(app.id)}
                  disabled={loadingHistoryAppId === app.id}
                >
                  {loadingHistoryAppId === app.id
                    ? "Loading..."
                    : historyByApplication[app.id]
                    ? "Hide History"
                    : "View History"}
                </button>

                {historyByApplication[app.id] && (
                  <div className="history-block">
                    {historyByApplication[app.id].length === 0 ? (
                      <p>No audit entries yet.</p>
                    ) : (
                      historyByApplication[app.id].map((entry) => (
                        <p className="history-line" key={entry.id}>
                          {entry.oldStatus || "START"} {" -> "} {entry.newStatus} by {entry.changedBy} at{" "}
                          {new Date(entry.changedAt).toLocaleString()}
                        </p>
                      ))
                    )}
                  </div>
                )}
              </article>
            ))}
          </div>
        )}
      </section>
    </main>
  );
}

export default App;
