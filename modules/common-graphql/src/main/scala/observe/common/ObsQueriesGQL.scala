// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package observe.common

import clue.GraphQLOperation
import clue.annotation.GraphQL
import lucuma.schemas.ObservationDB
import lucuma.core.enums.Site
import io.circe.{Decoder, Encoder}
import io.circe.generic.auto.*
import lucuma.core.math
import lucuma.core.enums
import lucuma.core.model
import cats.syntax.functor.*
import lucuma.core.model.sequence.{Atom, ExecutionSequence, GmosGratingConfig, StaticConfig, Step}

import java.time
import lucuma.core.model.{ExecutionEvent, Observation, Target}

// gql: import lucuma.schemas.decoders._
// gql: import io.circe.refined._

object ObsQueriesGQL {

  // I don't know why, but these implicits prevent several warnings in the generated code
//  implicit val obsIdCodex: Decoder[Observation.Id] with Encoder[Observation.Id]         =
//    Observation.Id.GidId
//  implicit val atomIdCodex: Decoder[Atom.Id] with Encoder[Atom.Id]                      = Atom.Id.UidId
//  implicit val stepIdCodex: Decoder[Step.Id] with Encoder[Step.Id]                      = Step.Id.UidId
//  implicit val targetIdCodex: Decoder[Target.Id] with Encoder[Target.Id]                = Target.Id.GidId
//  implicit val eventIdCodex: Decoder[ExecutionEvent.Id] with Encoder[ExecutionEvent.Id] =
//    ExecutionEvent.Id.GidId

  @GraphQL
  trait ActiveObservationIdsQuery extends GraphQLOperation[ObservationDB] {
    val document = """
      query {
        observations(WHERE: { status: { eq: { EQ: READY } } }) {
          matches {
            id
            title
          }
        }
      }
    """
  }

  @GraphQL
  trait ObsQuery extends GraphQLOperation[ObservationDB] {
    val document = """
      query($obsId: ObservationId!) {
        observation(observationId: $obsId) {
          id
          title
          status
          activeStatus
          plannedTime {
            execution {
              microseconds
            }
          }
          targetEnvironment {
            firstScienceTarget {
              targetId: id
              targetName: name
            }
          }
          constraintSet {
            imageQuality
            cloudExtinction
            skyBackground
            waterVapor
          }
          execution {
            config:executionConfig {
              instrument
              ... on GmosNorthExecutionConfig {
                staticN:static {
                  stageMode
                  detector
                  mosPreImaging
                  nodAndShuffle {
                    ...nodAndShuffle
                  }
                }
                acquisitionN:acquisition {
                  ...gmosNorthExecutionSequence
                }
                scienceN:science {
                  ...gmosNorthExecutionSequence
                }
              }
              ... on GmosSouthExecutionConfig {
                staticS:static {
                  stageMode
                  detector
                  mosPreImaging
                  nodAndShuffle {
                    ...nodAndShuffle
                  }
                }
                acquisitionS: acquisition {
                  ...gmosSouthExecutionSequence
                }
                scienceS:science {
                  ...gmosSouthExecutionSequence
                }
              }
            }
          }
        }
      }

      fragment northAtomFields on GmosNorthAtom {
        id
        steps {
          id
          stepConfig {
            ...stepConfig
          }
          instrumentConfig {
            exposure {
              microseconds
            }
            readout {
              ...gmosCcdMode
            }
            dtax
            roi
            gratingConfig {
              grating
              order
              wavelength {
                picometers
              }
            }
            filter
            fpu {
              builtin
              customMask {
                filename
                slitWidth
              }
            }
          }
        }
      }

      fragment southAtomFields on GmosSouthAtom {
        id
        steps {
          id
          stepConfig {
            ...stepConfig
          }
          instrumentConfig {
            exposure {
              microseconds
            }
            readout {
              ...gmosCcdMode
            }
            dtax
            roi
            gratingConfig {
              grating
              order
              wavelength {
                picometers
              }
            }
            filter
            fpu {
              builtin
              customMask {
                filename
                slitWidth
              }
            }
          }
        }
      }

      fragment offset on Offset {
        p {
          microarcseconds
        }
        q {
          microarcseconds
        }
      }

      fragment nodAndShuffle on GmosNodAndShuffle {
        posA {
          ...offset
        }
        posB {
          ...offset
        }
        eOffset
        shuffleOffset
        shuffleCycles
      }

      fragment gmosNorthExecutionSequence on GmosNorthExecutionSequence {
        nextAtom {
          ...northAtomFields
        }
        possibleFuture {
          ...northAtomFields
        }
      }

      fragment gmosSouthExecutionSequence on GmosSouthExecutionSequence {
        nextAtom {
          ...southAtomFields
        }
        possibleFuture {
          ...southAtomFields
        }
      }

      fragment gmosCcdMode on GmosCcdMode {
        xBin
        yBin
        ampCount
        ampGain
        ampReadMode
      }

      fragment gcal on Gcal {
        filter
        diffuser
        shutter
      }

      fragment stepConfig on StepConfig {
        stepType
        ... on Gcal {
          ...gcal
        }
        ... on Science {
          offset {
            ...offset
          }
        }
        ... on Bias {
        }
        ... on Dark {
        }
      }

    """

    given [T]: Decoder[math.Offset.Component[T]] =
      Decoder.instance(c =>
        c.downField("microarcseconds")
          .as[Long]
          .map(
            math.Angle.signedMicroarcseconds.reverse
              .andThen(math.Offset.Component.angle[T].reverse)
              .get
          )
      )

    given Decoder[math.Offset] = Decoder.instance(c =>
      for {
        p <- c.downField("p").as[math.Offset.P]
        q <- c.downField("q").as[math.Offset.Q]
      } yield math.Offset(p, q)
    )

//    implicit val seqStepConfigDecoder: Decoder[SeqStepConfig] = List[Decoder[SeqStepConfig]](
//      Decoder[SeqStepConfig.SeqScienceStep].widen,
//      Decoder[SeqStepConfig.Gcal].widen,
//      Decoder[SeqStepConfig.Bias].widen,
//      Decoder[SeqStepConfig.Dark].widen
//    ).reduceLeft(_ or _)

//    given Decoder[GmosSite] = List[Decoder[GmosSite]](
//      Decoder[Site.GN.type].widen,
//      Decoder[Site.GS.type].widen
//    ).reduceLeft(_ or _)

//    def seqFpuDecoder[S <: Site](using
//      d1: Decoder[GmosFpu.GmosBuiltinFpu[S]],
//      d2: Decoder[GmosFpu.GmosCustomMask[S]]
//    ): Decoder[GmosFpu[S]] = List[Decoder[GmosFpu[S]]](
//      Decoder[GmosFpu.GmosBuiltinFpu[S]].widen,
//      Decoder[GmosFpu.GmosCustomMask[S]].widen
//    ).reduceLeft(_ or _)

//    implicit val fpuSouthDecoder: Decoder[GmosFpu[Site.GS.type]] = seqFpuDecoder[Site.GS.type]
//    implicit val fpuNorthDecoder: Decoder[GmosFpu[Site.GN.type]] = seqFpuDecoder[Site.GN.type]

    object Data {
      object Observation {
        object Execution {
          object Config {
            object GmosNorthExecutionConfig {
              type StaticN = StaticConfig.GmosNorth

              type AcquisitionN = ExecutionSequence.GmosNorth

              type ScienceN = ExecutionSequence.GmosNorth
            }

            object GmosSouthExecutionConfig {
              type StaticS = StaticConfig.GmosSouth

              type AcquisitionS = ExecutionSequence.GmosSouth

              type ScienceS = ExecutionSequence.GmosSouth
            }
          }
        }
      }
    }
  }

  @GraphQL
  trait ProgramObservationsEditSubscription extends GraphQLOperation[ObservationDB] {
    val document = """
      subscription {
        observationEdit(programId:"p-2") {
          id
        }
      }
    """
  }

  @GraphQL
  trait ObservationEditSubscription extends GraphQLOperation[ObservationDB] {
    val document = """
      subscription($obsId: ObservationId!) {
        observationEdit(observationId: $obsId) {
          id
        }
      }
    """
  }

  @GraphQL
  trait AddSequenceEventMutation extends GraphQLOperation[ObservationDB] {
    val document = """
      mutation($vId: VisitId!, $obsId: ObservationId!, $cmd: SequenceCommand!) {
        addSequenceEvent(input: { visitId: $vId, location: { observationId: $obsId }, payload: { command: $cmd } } ) {
          event {
            visitId
            received
          }
        }
      }
      """
  }

  @GraphQL
  trait AddStepEventMutation extends GraphQLOperation[ObservationDB] {
    val document = """
      mutation($vId: VisitId!, $obsId: ObservationId!, $stpId: StepId!, $seqType: SequenceType!, $stg: StepStage!)  {
        addStepEvent(input: { visitId: $vId, location: { observationId: $obsId, stepId: $stpId }, payload: { sequenceType: $seqType, stage: $stg } } ) {
          event {
            received
          }
        }
      }
      """
  }

  @GraphQL
  trait AddDatasetEventMutation extends GraphQLOperation[ObservationDB] {
    val document = """
      mutation($vId: VisitId!, $obsId: ObservationId!, $stpId: StepId!, $dtIdx: PosInt!, $stg: DatasetStage!, $flName: DatasetFilename)  {
        addDatasetEvent(input: { visitId: $vId, location: { observationId: $obsId, stepId: $stpId, index: $dtIdx }, payload: { datasetStage: $stg, filename: $flName } } ) {
          event {
            received
          }
        }
      }
      """
  }

}
