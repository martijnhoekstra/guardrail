package dev.guardrail.generators.spi

import cats.Applicative
import cats.Monoid
import cats.Semigroup
import cats.data.NonEmptyList
import cats.implicits._

object ModuleLoadResult {
  implicit val moduleLoadResultApplicative: Applicative[ModuleLoadResult] = new Applicative[ModuleLoadResult] {
    def pure[A](x: A): ModuleLoadResult[A] = new ModuleLoadSuccess(Set.empty, Set.empty, x)
    def ap[A, B](ff: ModuleLoadResult[A => B])(fa: ModuleLoadResult[A]): ModuleLoadResult[B] = ff match {
      case f: ModuleLoadSuccess[A => B] =>
        fa match {
          case a: ModuleLoadSuccess[A] => new ModuleLoadSuccess[B](ff.attempted.combine(fa.attempted), f.consumed.combine(a.consumed), f.result(a.result))
          case fail: ModuleLoadFailed  => fail
        }
      case ffail: ModuleLoadFailed =>
        fa match {
          case afail: ModuleLoadFailed =>
            new ModuleLoadFailed(ffail.attempted.combine(afail.attempted), ffail.missing.combine(afail.missing))
          case _ => ffail
        }
    }
  }

  implicit def moduleLoadResultSemigroup[A]: Monoid[ModuleLoadResult[A]] = new Monoid[ModuleLoadResult[A]] {
    def empty: ModuleLoadResult[A] = new ModuleLoadFailed(Set.empty, Set.empty, Map.empty)
    def combine(a: ModuleLoadResult[A], b: ModuleLoadResult[A]): ModuleLoadResult[A] =
      (a, b) match {
        case (a: ModuleLoadSuccess[A], b: ModuleLoadSuccess[A]) =>
          new ModuleLoadSuccess(a.attempted.combine(b.attempted), a.consumed.combine(b.consumed), a.result)
        case (a: ModuleLoadFailed, b: ModuleLoadSuccess[A]) =>
          new ModuleLoadSuccess(a.attempted.combine(b.attempted), b.consumed, b.result)
        case (a: ModuleLoadSuccess[A], b: ModuleLoadFailed) =>
          new ModuleLoadSuccess(Set.empty, Set.empty, a.result)
        case (a: ModuleLoadFailed, b: ModuleLoadFailed) =>
          new ModuleLoadFailed(a.attempted.combine(b.attempted), a.missing.combine(b.missing))
      }
  }

  private[this] def wrapper[Modules, Pivot](
      extract: String => Pivot
  )(
      func: Pivot => ModuleLoadResult[Modules]
  )(implicit ev1: Monoid[ModuleLoadResult[Modules]], ev2: Semigroup[Pivot]): Set[String] => Option[ModuleLoadResult[Modules]] = { modules =>
    NonEmptyList
      .fromList(modules.toList.sorted)
      .map { nel =>
        func(nel.map(extract).reduce)
      }
  }

  type ModuleDescriptor[A] = (String, Seq[Map[String, A]])

  private[this] def work[A](a: ModuleDescriptor[A]): String => ModuleLoadResult[A] = { module =>
    val (label, mappings) = a
    mappings.foldLeft[ModuleLoadResult[A]](new ModuleLoadFailed(Set.empty, Set(label))) { case (acc, next) =>
      acc.combine(
        next
          .get(module)
          .fold[ModuleLoadResult[A]](new ModuleLoadFailed(Set(module), Set.empty)) { res =>
            new ModuleLoadSuccess[A](Set(module), Set(module), res)
          }
      )
    }
  }

  private[this] def extractLabel[A](descriptor: ModuleDescriptor[A]): Set[String] = Set(descriptor._1)

  def emitDefault[A](a: A): Set[String] => ModuleLoadResult[A] = _ => new ModuleLoadSuccess[A](Set.empty, Set.empty, a)
  def forProduct1[A, Z](a: ModuleDescriptor[A])(combine: A => Z): Set[String] => ModuleLoadResult[Z] =
    wrapper(module => work[A](a)(module))(_.map(combine)).map(_.getOrElse(new ModuleLoadFailed(Set.empty, extractLabel(a))))
  def forProduct2[A, B, Z](a: ModuleDescriptor[A], b: ModuleDescriptor[B])(combine: (A, B) => Z): Set[String] => ModuleLoadResult[Z] =
    wrapper(module => (work[A](a)(module), work[B](b)(module)))(_.mapN(combine))
      .map(_.getOrElse(new ModuleLoadFailed(Set.empty, extractLabel(a).combine(extractLabel(b)))))
  def forProduct3[A, B, C, Z](
      a: ModuleDescriptor[A],
      b: ModuleDescriptor[B],
      c: ModuleDescriptor[C]
  )(combine: (A, B, C) => Z): Set[String] => ModuleLoadResult[Z] =
    wrapper(module => (work[A](a)(module), work[B](b)(module), work[C](c)(module)))(_.mapN(combine))
      .map(
        _.getOrElse(
          new ModuleLoadFailed(
            Set.empty,
            extractLabel(a).combine(extractLabel(b).combine(extractLabel(c)))
          )
        )
      )
  def forProduct4[A, B, C, D, Z](
      a: ModuleDescriptor[A],
      b: ModuleDescriptor[B],
      c: ModuleDescriptor[C],
      d: ModuleDescriptor[D]
  )(combine: (A, B, C, D) => Z): Set[String] => ModuleLoadResult[Z] =
    wrapper(module => (work[A](a)(module), work[B](b)(module), work[C](c)(module), work[D](d)(module)))(_.mapN(combine))
      .map(
        _.getOrElse(
          new ModuleLoadFailed(
            Set.empty,
            extractLabel(a).combine(extractLabel(b).combine(extractLabel(c).combine(extractLabel(d))))
          )
        )
      )
  def forProduct5[A, B, C, D, E, Z](
      a: ModuleDescriptor[A],
      b: ModuleDescriptor[B],
      c: ModuleDescriptor[C],
      d: ModuleDescriptor[D],
      e: ModuleDescriptor[E]
  )(combine: (A, B, C, D, E) => Z): Set[String] => ModuleLoadResult[Z] =
    wrapper(module => (work[A](a)(module), work[B](b)(module), work[C](c)(module), work[D](d)(module), work[E](e)(module)))(_.mapN(combine))
      .map(
        _.getOrElse(
          new ModuleLoadFailed(
            Set.empty,
            extractLabel(a).combine(extractLabel(b).combine(extractLabel(c).combine(extractLabel(d).combine(extractLabel(e)))))
          )
        )
      )
  def forProduct6[A, B, C, D, E, F, Z](
      a: ModuleDescriptor[A],
      b: ModuleDescriptor[B],
      c: ModuleDescriptor[C],
      d: ModuleDescriptor[D],
      e: ModuleDescriptor[E],
      f: ModuleDescriptor[F]
  )(combine: (A, B, C, D, E, F) => Z): Set[String] => ModuleLoadResult[Z] =
    wrapper(module =>
      (
        work[A](a)(module),
        work[B](b)(module),
        work[C](c)(module),
        work[D](d)(module),
        work[E](e)(module),
        work[F](f)(module)
      )
    )(_.mapN(combine)).map(
      _.getOrElse(
        new ModuleLoadFailed(
          Set.empty,
          extractLabel(a).combine(extractLabel(b).combine(extractLabel(c).combine(extractLabel(d).combine(extractLabel(e).combine(extractLabel(f))))))
        )
      )
    )

  def buildFrom[Modules, Extracted[_]](extract: String => Extracted[Modules])(implicit ev1: Monoid[Extracted[Modules]]): Set[String] => Extracted[Modules] = {
    parameters =>
      NonEmptyList
        .fromList(parameters.toList)
        .fold(Monoid[Extracted[Modules]].empty) { nel =>
          nel.map(extract).reduce
        }
  }
}

// Parameters:
// - attempted: All mapping keys for the current stage
sealed abstract class ModuleLoadResult[+A](val attempted: Set[String])

class ModuleLoadFailed(attempted: Set[String], val missing: Set[String]) extends ModuleLoadResult[Nothing](attempted) {
  override def toString(): String = s"ModuleLoadFailed($attempted, $missing)"
}

class ModuleLoadSuccess[A](attempted: Set[String], val consumed: Set[String], val result: A) extends ModuleLoadResult[A](attempted) {
  override def toString(): String = s"ModuleLoadSuccess($attempted, $consumed, $result)"
}
