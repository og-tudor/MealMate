import androidx.lifecycle.ViewModel
import android.net.Uri

data class Category(val name: String, val imageUri: Uri?, val id: String)

class CategoriesViewModel : ViewModel() {
    val cachedCategories = mutableListOf<Category>()
    // Flag to track if the layout was already created
    var layoutInitialized = false
}
