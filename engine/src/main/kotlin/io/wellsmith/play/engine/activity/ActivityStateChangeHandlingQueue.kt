package io.wellsmith.play.engine.activity

interface ActivityStateChangeHandlingQueue {

  fun queue(activityStateChange: ActivityStateChange)
}