import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicacionlesaa.adapter.muestraAdapter

class DragManageAdapter(
    private val adapter: muestraAdapter,
    private val isEditMode: () -> Boolean
) : ItemTouchHelper.Callback() {

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = if (isEditMode()) 0 else ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val fromPosition = viewHolder.adapterPosition
        val toPosition = target.adapterPosition
        adapter.onItemMove(fromPosition, toPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // No necesitas implementar swipe
    }

    override fun isLongPressDragEnabled(): Boolean {
        return true
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            val itemView = viewHolder.itemView
            val scaleFactor = 1.05f // Escalar ligeramente el elemento
            itemView.scaleX = scaleFactor
            itemView.scaleY = scaleFactor
            itemView.alpha = 0.8f // Cambiar la opacidad
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

        if (!isCurrentlyActive) {
            val itemView = viewHolder.itemView
            itemView.scaleX = 1f
            itemView.scaleY = 1f
            itemView.alpha = 1f
        }
    }
}
