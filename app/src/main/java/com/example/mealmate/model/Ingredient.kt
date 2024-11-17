import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Ingredient(
    val name: String = "", // Default value for Firestore compatibility
    val quantity: String = "" // Default value for Firestore compatibility
) : Parcelable {
    // Firestore requires a no-argument constructor
    constructor() : this("", "")
}
