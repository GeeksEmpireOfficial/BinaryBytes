package net.geekstools.numbers.GameView

class AnimationGridKotlin {

    internal var globalAnimation = ArrayList<AnimationCell>()
    //    private var field: Array<Array<AnimationCell>>
    private var field: Array<Array<ArrayList<AnimationCell>>>

    private var activeAnimations = 0
    private var oneMoreFrame = false

    constructor(x: Int, y: Int/*, animationType: Int, length: Long, delay: Long, extras: IntArray*/) {
        /*field = Array<Array<AnimationCell>>(x) {
            Array<AnimationCell>(y){
                AnimationCell(x, y, animationType, length, delay, extras)
            }
        }*/
        field = Array<Array<ArrayList<AnimationCell>>>(x) {
            Array<ArrayList<AnimationCell>>(y) {
                ArrayList<AnimationCell>(0)
            }
        }

        for (xx in 0 until x) {
            for (yy in 0 until y) {
                field[xx][yy] = ArrayList<AnimationCell>(0)
            }
        }
    }

    fun startAnimation(x: Int, y: Int, animationType: Int, length: Long, delay: Long, extras: IntArray) {
        val animationToAdd = AnimationCell(x, y, animationType, length, delay, extras)

        if (x == -1 && y == -1) {
            globalAnimation.add(animationToAdd)
        } else {
            field[x][y].add(animationToAdd)
        }
        activeAnimations += 1
    }

    fun tickAll(timeElapsed: Long) {
        val cancelledAnimations = ArrayList<AnimationCell>()
        for (animation in globalAnimation) {
            animation.tick(timeElapsed)
            if (animation.animationDone()) {
                cancelledAnimations.add(animation)
                activeAnimations -= 1
            }
        }

        for (array in field) {
            for (list in array) {
                for (animation in list) {
                    animation.tick(timeElapsed)
                    if (animation.animationDone()) {
                        cancelledAnimations.add(animation)
                        activeAnimations -= 1
                    }
                }
            }
        }

        for (animation in cancelledAnimations) {
            cancelAnimation(animation)
        }
    }

    fun isAnimationActive(): Boolean {
        if (activeAnimations != 0) {
            oneMoreFrame = true
            return true
        } else if (oneMoreFrame) {
            oneMoreFrame = false
            return true
        } else {
            return false
        }
    }

    internal fun getAnimationCell(x: Int, y: Int): ArrayList<AnimationCell> {
        return field[x][y]
    }

    fun cancelAnimations() {
        for (array in field) {
            for (list in array) {
                list.clear()
            }
        }
        globalAnimation.clear()
        activeAnimations = 0
    }

    private fun cancelAnimation(animation: AnimationCell) {
        if (animation.x == -1 && animation.y == -1) {
            globalAnimation.remove(animation)
        } else {
            field[animation.x][animation.y].remove(animation)
        }
    }
}
