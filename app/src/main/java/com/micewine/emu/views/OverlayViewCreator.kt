package com.micewine.emu.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.VectorDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.micewine.emu.R
import com.micewine.emu.activities.VirtualControllerOverlayMapper.Companion.ACTION_EDIT_VIRTUAL_BUTTON
import com.micewine.emu.adapters.AdapterPreset.Companion.clickedPresetName
import com.micewine.emu.fragments.EditVirtualButtonFragment.Companion.selectedAnalogDownKeyName
import com.micewine.emu.fragments.EditVirtualButtonFragment.Companion.selectedAnalogLeftKeyName
import com.micewine.emu.fragments.EditVirtualButtonFragment.Companion.selectedAnalogRightKeyName
import com.micewine.emu.fragments.EditVirtualButtonFragment.Companion.selectedAnalogUpKeyName
import com.micewine.emu.fragments.EditVirtualButtonFragment.Companion.selectedButtonKeyName
import com.micewine.emu.fragments.EditVirtualButtonFragment.Companion.selectedButtonRadius
import com.micewine.emu.fragments.VirtualControllerPresetManagerFragment.Companion.getMapping
import com.micewine.emu.fragments.VirtualControllerPresetManagerFragment.Companion.putMapping
import com.micewine.emu.views.OverlayView.Companion.analogList
import com.micewine.emu.views.OverlayView.Companion.buttonList
import com.micewine.emu.views.OverlayView.Companion.detectClick

class OverlayViewCreator @JvmOverloads constructor (context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0): View(context, attrs, defStyleAttr) {
    private val editButton: CircleButton = CircleButton(0F, 0F, 150F)
    private val removeButton: CircleButton = CircleButton(0F, 0F, 150F)

    private var editIcon: Bitmap = getBitmapFromVectorDrawable(context, R.drawable.ic_edit, (editButton.radius / 2).toInt(), (editButton.radius / 2).toInt())
    private var removeIcon: Bitmap = getBitmapFromVectorDrawable(context, R.drawable.ic_delete, (removeButton.radius / 2).toInt(), (removeButton.radius / 2).toInt())

    private fun getBitmapFromVectorDrawable(context: Context, drawableId: Int, width: Int, height: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, drawableId) as VectorDrawable
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private val paint = Paint().apply {
        color = Color.BLACK
        alpha = 200
    }

    private val whitePaint = Paint().apply {
        color = Color.WHITE
    }

    private val buttonPaint: Paint = Paint().apply {
        strokeWidth = 8F
        color = Color.WHITE
        style = Paint.Style.STROKE
    }

    private val textPaint: Paint = Paint().apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 40F
    }

    private var selectedButton = 0
    private var selectedVAxis = 0

    private fun loadFromPreferences() {
        val mapping = getMapping(clickedPresetName)

        buttonList.clear()
        analogList.clear()

        mapping?.buttons?.forEach {
            buttonList.add(it)
        }

        mapping?.analogs?.forEach {
            analogList.add(it)
        }

        reorderButtonsAnalogsIDs()
    }

    fun saveOnPreferences() {
        putMapping(clickedPresetName, buttonList, analogList)
    }

    init {
        loadFromPreferences()
    }

    private fun drawText(text: String, x: Float, y: Float, c: Canvas) {
        textPaint.style = Paint.Style.FILL
        textPaint.color = Color.WHITE

        c.drawText(text, x, y, textPaint)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        buttonList.forEach {
            buttonPaint.color = if (lastSelectedButton == it.id && lastSelectedType == BUTTON) Color.GRAY else Color.WHITE
            textPaint.color = buttonPaint.color

            buttonPaint.alpha = 220

            canvas.drawCircle(it.x, it.y, it.radius / 2, buttonPaint)

            paint.textSize = it.radius / 4

            drawText(it.keyName, it.x, it.y + 10, canvas)
        }

        analogList.forEach {
            buttonPaint.color = if (lastSelectedButton == it.id && lastSelectedType == ANALOG) Color.GRAY else Color.WHITE
            whitePaint.color = buttonPaint.color

            whitePaint.alpha = 220
            buttonPaint.alpha = 220

            canvas.apply {
                drawCircle(it.x, it.y, it.radius / 2, buttonPaint)
                drawCircle(it.x, it.y, it.radius / 4, whitePaint)
            }
        }

        if (lastSelectedButton > 0) {
            editButton.x = width - 20F - editButton.radius / 2
            editButton.y = 20F + editButton.radius / 2

            removeButton.x = editButton.x - removeButton.radius
            removeButton.y = 20F + removeButton.radius / 2

            canvas.apply {
                drawCircle(editButton.x, editButton.y, editButton.radius / 2, paint)
                drawCircle(removeButton.x, removeButton.y, removeButton.radius / 2, paint)
                drawBitmap(editIcon, editButton.x - editButton.radius / 4, editButton.y - editButton.radius / 4, whitePaint)
                drawBitmap(removeIcon, removeButton.x - removeButton.radius / 4, removeButton.y - removeButton.radius / 4, whitePaint)
            }
        }
    }

    fun addButton(buttonData: OverlayView.VirtualButton) {
        buttonList.add(buttonData)
        invalidate()
    }

    fun addAnalog(buttonData: OverlayView.VirtualAnalog) {
        analogList.add(buttonData)
        invalidate()
    }

    private fun reorderButtonsAnalogsIDs() {
        buttonList.forEachIndexed { i, button ->
            button.id = i + 1
        }

        analogList.forEachIndexed { i, analog ->
            analog.id = i + 1
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
         when (event.actionMasked) {
             MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                 if (!detectClick(event, event.actionIndex, editButton.x, editButton.y, editButton.radius) && !detectClick(event, event.actionIndex, removeButton.x, removeButton.y, removeButton.radius)) {
                     lastSelectedButton = 0
                 }

                 buttonList.forEach {
                     if (detectClick(event, event.actionIndex, it.x, it.y, it.radius)) {
                         if (selectedButton == 0) {
                             selectedButton = it.id
                             lastSelectedType = BUTTON
                             lastSelectedButton = it.id
                         }
                     }
                 }

                 analogList.forEach {
                     if (detectClick(event, event.actionIndex, it.x, it.y, it.radius)) {
                         if (selectedVAxis == 0) {
                             selectedVAxis = it.id
                             lastSelectedType = ANALOG
                             lastSelectedButton = it.id
                         }
                     }
                 }

                 invalidate()
             }

             MotionEvent.ACTION_MOVE -> {
                 buttonList.forEach {
                     if (detectClick(event, event.actionIndex, it.x, it.y, it.radius)) {
                         if (selectedButton > 0) {
                             buttonList[buttonList.indexOfFirst { i ->
                                 i.id == selectedButton
                             }].apply {
                                 x = event.getX(event.actionIndex)
                                 y = event.getY(event.actionIndex)
                             }
                         }
                     }
                 }

                 analogList.forEach {
                     if (detectClick(event, event.actionIndex, it.x, it.y, it.radius)) {
                         if (selectedVAxis > 0) {
                             analogList[analogList.indexOfFirst { i ->
                                 i.id == selectedVAxis
                             }].apply {
                                 x = event.getX(event.actionIndex)
                                 y = event.getY(event.actionIndex)
                             }
                         }
                     }
                 }

                 invalidate()
             }

             MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP -> {
                 buttonList.forEach {
                     if (detectClick(event, event.actionIndex, it.x, it.y, it.radius)) {
                         if (selectedButton == it.id) {
                             selectedButton = 0
                         }
                     }
                 }

                 analogList.forEach {
                     if (detectClick(event, event.actionIndex, it.x, it.y, it.radius)) {
                         if (selectedVAxis == it.id) {
                             selectedVAxis = 0
                         }
                     }
                 }

                 if (detectClick(event, event.actionIndex, editButton.x, editButton.y, editButton.radius) && lastSelectedButton > 0) {
                     if (buttonList.isNotEmpty() && lastSelectedType == BUTTON) {
                         selectedButtonKeyName = buttonList[lastSelectedButton - 1].keyName
                         selectedButtonRadius = buttonList[lastSelectedButton - 1].radius.toInt()
                     }

                     if (analogList.isNotEmpty() && lastSelectedType == ANALOG) {
                         selectedAnalogUpKeyName = analogList[lastSelectedButton - 1].upKeyName
                         selectedAnalogDownKeyName = analogList[lastSelectedButton - 1].downKeyName
                         selectedAnalogLeftKeyName = analogList[lastSelectedButton - 1].leftKeyName
                         selectedAnalogRightKeyName = analogList[lastSelectedButton - 1].rightKeyName
                         selectedButtonRadius = analogList[lastSelectedButton - 1].radius.toInt()
                     }

                     context.sendBroadcast(
                         Intent(ACTION_EDIT_VIRTUAL_BUTTON)
                     )
                 }

                 if (detectClick(event, event.actionIndex, removeButton.x, removeButton.y, removeButton.radius) && lastSelectedButton > 0) {
                     if (buttonList.isNotEmpty() && lastSelectedType == BUTTON) {
                         buttonList.removeAt(lastSelectedButton - 1)
                     }

                     if (analogList.isNotEmpty() && lastSelectedType == ANALOG) {
                         analogList.removeAt(lastSelectedButton - 1)
                     }

                     lastSelectedButton = 0

                     reorderButtonsAnalogsIDs()
                     invalidate()
                 }
             }
         }

        return true
    }

    class CircleButton(
        var x: Float,
        var y: Float,
        var radius: Float,
    )

    companion object {
        const val BUTTON = 0
        const val ANALOG = 1

        var lastSelectedButton = 0
        var lastSelectedType = BUTTON
    }
}
