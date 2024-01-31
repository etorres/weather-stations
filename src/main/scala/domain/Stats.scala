package es.eriktorr.weather
package domain

final case class Stats(count: Long, min: Double, max: Double, sum: Double):
  def mean: Double = sum / count.toDouble
