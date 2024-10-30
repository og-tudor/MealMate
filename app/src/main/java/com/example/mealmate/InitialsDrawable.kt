import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable

class InitialsDrawable(context: Context, private val letter: String) : ColorDrawable() {

    private val paint: Paint = Paint().apply {
        color = Color.WHITE  // Text color
        textSize = 40f       // Adjust text size as needed
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        // Calculate the position to center the text
        val bounds = Rect()
        paint.getTextBounds(letter, 0, letter.length, bounds)

        val x = (bounds.width() / 2f) + (canvas.width / 2f) - bounds.exactCenterX()
        val y = (canvas.height / 2f) - (paint.descent() + paint.ascent()) / 2f

        // Draw the text at the calculated position
        canvas.drawText(letter, x, y, paint)
    }
}
