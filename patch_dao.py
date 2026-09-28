with open("app/src/main/java/com/example/itantra/data/TriageDao.kt", "r", encoding="utf-8") as f:
    code = f.read()
code = code.replace("    suspend fun markAsAcked(msgId: String)", "    suspend fun markAsAcked(msgId: String)\n\n    @Query(\"SELECT isAcked FROM victims WHERE id = :msgId\")\n    suspend fun isAcked(msgId: String): Boolean?")
with open("app/src/main/java/com/example/itantra/data/TriageDao.kt", "w", encoding="utf-8") as f:
    f.write(code)

with open("app/src/main/java/com/example/itantra/data/TriageRepository.kt", "r", encoding="utf-8") as f:
    code = f.read()
code = code.replace("    suspend fun markAsAcked(msgId: String) {\n        triageDao.markAsAcked(msgId)\n    }", "    suspend fun markAsAcked(msgId: String) {\n        triageDao.markAsAcked(msgId)\n    }\n\n    suspend fun isAcked(msgId: String): Boolean {\n        return triageDao.isAcked(msgId) ?: false\n    }")
with open("app/src/main/java/com/example/itantra/data/TriageRepository.kt", "w", encoding="utf-8") as f:
    f.write(code)
