package org.coursera.autoschema
package annotations

import scala.annotation.meta.field

object Term {
  type Description = annotations.Description @field
  type ExposeAs    = annotations.ExposeAs @field
  type FormatAs    = annotations.FormatAs @field
  type Hide        = annotations.Hide @field
}
