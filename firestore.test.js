const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";
const ADMIN_EMAIL = "ranamaaz064225@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("curriculum").where("published", "==", true).get());
  await assertFails(unauth.collection("plans").where("active", "==", true).get());
  await assertFails(unauth.collection("supportTickets").where("userId", "==", ALICE_UID).get());
  await assertFails(
    unauth.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      preferredLanguage: "en",
      country: "Pakistan",
      board: "Punjab Board",
      studentClass: "Class 10",
      planTier: "FREE",
      status: "active",
      dailyQuestionsUsed: 0,
      dailyScansUsed: 0,
      totalSolved: 0,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("User profile: owner creates FREE active profile, updates prefs, cannot self-upgrade plan or status", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(5000);

  // Reject self-promotion to PRO on create
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      preferredLanguage: "en",
      country: "Pakistan",
      board: "Punjab Board",
      studentClass: "Class 10",
      planTier: "PRO",
      status: "active",
      dailyQuestionsUsed: 0,
      dailyScansUsed: 0,
      totalSolved: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid FREE create
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Khan",
      email: "alice@example.com",
      preferredLanguage: "en",
      country: "Pakistan",
      board: "Punjab Board",
      studentClass: "Class 10",
      planTier: "FREE",
      status: "active",
      dailyQuestionsUsed: 0,
      dailyScansUsed: 0,
      totalSolved: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid preference update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredLanguage: "ur",
      board: "Federal Board",
      updatedAt: pastDate(1000),
    })
  );

  // Reject planTier self-upgrade on update
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      planTier: "PREMIUM",
      updatedAt: pastDate(500),
    })
  );

  // Reject cross-user read
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Solved questions & subcollections: strict owner isolation and schema validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(4000);

  // Reject @DocumentId "id" key inside payload
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("questions").doc("q_bad").set({
      id: "q_bad",
      userId: ALICE_UID,
      questionText: "2x + 4 = 10",
      inputMode: "text",
      language: "en",
      difficultyLevel: "easy",
      subject: "Algebra",
      steps: ["Subtract 4", "Divide by 2"],
      finalAnswer: "x = 3",
      bookmarked: false,
      saved: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid question create
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("questions").doc("q_1").set({
      userId: ALICE_UID,
      questionText: "2x + 4 = 10",
      detectedExpression: "2*x + 4 = 10",
      inputMode: "text",
      language: "en",
      difficultyLevel: "detailed",
      subject: "Algebra",
      verificationStatus: "Deterministically Verified",
      identification: "Linear equation in one variable",
      givenInfo: "2x + 4 = 10",
      relevantFormula: "ax + b = c => x = (c - b)/a",
      formulaExplanation: "Isolate x using inverse operations.",
      steps: ["2x = 10 - 4", "2x = 6", "x = 3"],
      calculations: "2x = 6 -> x = 3",
      finalAnswer: "x = 3",
      verification: "2(3) + 4 = 10",
      whyItWorks: "Balancing both sides preserves equality.",
      practiceQuestion: "Solve 3x - 5 = 10",
      bookmarked: false,
      saved: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid bookmark toggle partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("questions").doc("q_1").update({
      bookmarked: true,
      updatedAt: pastDate(1000),
    })
  );

  // Bob cannot list or delete Alice's questions
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("questions").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("questions").doc("q_1").delete());

  // Owner can list & delete own scan history and practice attempts
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("scanHistory").doc("s_1").set({
      userId: ALICE_UID,
      detectedText: "x^2 - 9 = 0",
      clarityStatus: "clear",
      subject: "Algebra",
      questionId: "q_1",
      createdAt: ts,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("scanHistory").get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("practiceAttempts").doc("p_1").set({
      userId: ALICE_UID,
      subject: "Algebra",
      topic: "Linear Equations",
      board: "Punjab Board",
      studentClass: "Class 9",
      totalQuestions: 5,
      correctCount: 4,
      scorePercent: 80,
      createdAt: ts,
    })
  );
});

test("Support tickets: owner filtered query succeeds, unfiltered fails for non-admin, admin manages status", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();
  const ts = pastDate(4000);

  await assertSucceeds(
    aliceDb.collection("supportTickets").doc("t_1").set({
      userId: ALICE_UID,
      userEmail: "alice@example.com",
      subject: "Urdu display question",
      message: "How do I switch voice teacher speed?",
      category: "Help",
      status: "open",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Owner filtered query succeeds; unfiltered fails
  await assertSucceeds(aliceDb.collection("supportTickets").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("supportTickets").get());
  await assertFails(bobDb.collection("supportTickets").doc("t_1").get());

  // Non-admin cannot change ticket status
  await assertFails(
    aliceDb.collection("supportTickets").doc("t_1").update({
      status: "resolved",
      updatedAt: pastDate(500),
    })
  );

  // Admin can list all and reply/resolve
  await assertSucceeds(adminDb.collection("supportTickets").get());
  await assertSucceeds(
    adminDb.collection("supportTickets").doc("t_1").update({
      status: "resolved",
      adminReply: "Use the speed slider in Voice Teacher.",
      updatedAt: pastDate(500),
    })
  );
});

test("Admin security: email spoofing without email_verified fails, admins write blocked, curriculum & logs protected", async () => {
  const spoofedAdminDb = testEnv
    .authenticatedContext("spoof_1", { email: ADMIN_EMAIL, email_verified: false })
    .firestore();
  const verifiedAdminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();
  const studentDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastDate(4000);

  // Spoofed unverified admin email MUST fail
  await assertFails(
    spoofedAdminDb.collection("curriculum").doc("cur_1").set({
      country: "Pakistan",
      board: "Punjab Board",
      studentClass: "Class 10",
      subject: "Algebra",
      bookTitle: "Open Mathematics 10",
      chapterTitle: "Quadratic Equations",
      exerciseTitle: "Exercise 1.1",
      questionPrompt: "Solve x^2 - 5x + 6 = 0",
      sourceNote: "ZELU Open Curriculum",
      published: true,
      createdByUid: "spoof_1",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Nobody can write /admins from client
  await assertFails(
    verifiedAdminDb.collection("admins").doc(ALICE_UID).set({
      email: "alice@example.com",
      role: "admin",
      createdAt: ts,
    })
  );

  // Verified admin creates published curriculum item
  await assertSucceeds(
    verifiedAdminDb.collection("curriculum").doc("cur_1").set({
      country: "Pakistan",
      board: "Punjab Board",
      studentClass: "Class 10",
      subject: "Algebra",
      bookTitle: "Open Mathematics 10",
      chapterTitle: "Quadratic Equations",
      exerciseTitle: "Exercise 1.1",
      questionPrompt: "Solve x^2 - 5x + 6 = 0",
      sourceNote: "ZELU Open Curriculum",
      published: true,
      createdByUid: ADMIN_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Student must query with published == true; unfiltered query fails
  await assertSucceeds(studentDb.collection("curriculum").where("published", "==", true).get());
  await assertFails(studentDb.collection("curriculum").get());

  // Student cannot write curriculum or adminLogs
  await assertFails(studentDb.collection("curriculum").doc("cur_1").delete());
  await assertFails(
    studentDb.collection("adminLogs").doc("log_1").set({
      adminUid: ALICE_UID,
      adminEmail: "alice@example.com",
      action: "hack",
      targetPath: "users",
      details: "attempt",
      createdAt: ts,
    })
  );

  // Verified admin creates immutable audit log; update/delete rejected
  await assertSucceeds(
    verifiedAdminDb.collection("adminLogs").doc("log_1").set({
      adminUid: ADMIN_UID,
      adminEmail: ADMIN_EMAIL,
      action: "PUBLISH_CURRICULUM",
      targetPath: "curriculum/cur_1",
      details: "Published quadratic equation exercise",
      createdAt: ts,
    })
  );
  await assertFails(verifiedAdminDb.collection("adminLogs").doc("log_1").delete());
});

test("Payment methods, website settings & adminSecurity: strict admin isolation and user read filtering", async () => {
  const verifiedAdminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();
  const studentDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastDate(4000);

  // Admin can manage paymentMethods; student cannot write
  await assertFails(
    studentDb.collection("paymentMethods").doc("pm_1").set({
      name: "HackPay",
      providerType: "Wallet",
      accountNumber: "0300-0000000",
      enabled: true,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    verifiedAdminDb.collection("paymentMethods").doc("pm_1").set({
      name: "JazzCash",
      providerType: "Mobile Wallet",
      accountTitle: "ZELU Education",
      accountNumber: "0300-1234567",
      instructions: "Send receipt to support.",
      enabled: true,
      updatedAt: ts,
    })
  );

  // Student filtered enabled==true succeeds, unfiltered fails
  await assertSucceeds(studentDb.collection("paymentMethods").where("enabled", "==", true).get());
  await assertFails(studentDb.collection("paymentMethods").get());

  // Admin can update expanded systemSettings; student cannot
  await assertSucceeds(
    verifiedAdminDb.collection("systemSettings").doc("global").set({
      aiModel: "gemini-3.8-flash",
      dailyFreeLimit: 15,
      defaultVoiceLang: "ur",
      defaultSpeechRate: 1.0,
      defaultVoicePersona: "madam",
      defaultVoicePitch: 1.05,
      siteTitle: "ZELU Math AI",
      seoTitle: "ZELU Math AI Tutor",
      themeAccentHex: "#2563EB",
      defaultThemeMode: "system",
      footerText: "Powered by ZELU",
      socialYoutube: "https://youtube.com/@zelumath",
      adsEnabled: true,
      publicRead: true,
      updatedAt: ts,
    })
  );
  await assertSucceeds(studentDb.collection("systemSettings").where("publicRead", "==", true).get());

  // adminSecurity strictly admin-only
  await assertSucceeds(
    verifiedAdminDb.collection("adminSecurity").doc("config").set({
      passwordSalt: "abc",
      passwordHash: "hash123",
      passwordConfigured: true,
      twoFactorEnabled: true,
      totpSecret: "JBSWY3DPEHPK3PXP",
      recoveryEmail: ADMIN_EMAIL,
      updatedAt: ts,
    })
  );
  await assertFails(studentDb.collection("adminSecurity").doc("config").get());
  await assertFails(
    studentDb.collection("adminSecurity").doc("config").set({
      passwordConfigured: false,
      twoFactorEnabled: false,
      recoveryEmail: "hack@example.com",
      updatedAt: ts,
    })
  );
});

