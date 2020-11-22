import com.google.gson.JsonObject

data class BulbResult(
        val id: Int,
        val result: List<String>,
        val error: HashMap<String, String>,
        val method: String,
        val params: JsonObject
)