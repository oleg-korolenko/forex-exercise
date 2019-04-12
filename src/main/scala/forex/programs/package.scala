package forex

package object programs {
  type RatesProgram[F[_]] = rates.Algebra[F]
  final val RatesProgram = rates.Program

  type ProgramError = rates.errors.Error
}
