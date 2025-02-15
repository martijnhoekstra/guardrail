package tests.generators.akkaHttp.client

import dev.guardrail.generators.scala.ScalaGeneratorMappings.scalaInterpreter
import dev.guardrail.generators.scala.syntax.companionForStaticDefns
import dev.guardrail.Context
import dev.guardrail.generators.{ Client, Clients }
import support.SwaggerSpecRunner
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class DefaultParametersTest extends AnyFunSuite with Matchers with SwaggerSpecRunner {
  import scala.meta._

  val swagger = s"""
    |swagger: '2.0'
    |host: petstore.swagger.io
    |paths:
    |  "/store/order/{order_id}":
    |    get:
    |      tags:
    |      - store
    |      x-jvm-package: store
    |      operationId: getOrderById
    |      produces:
    |      - application/xml
    |      - application/json
    |      parameters:
    |      - name: order_id
    |        in: path
    |        required: true
    |        type: integer
    |        maximum: 10
    |        minimum: 1
    |        format: int64
    |      - name: HeaderMeThis
    |        in: header
    |        type: string
    |        required: true
    |      - name: defparm_opt
    |        in: query
    |        type: integer
    |        format: int32
    |        default: 1
    |      - name: defparm
    |        in: query
    |        type: integer
    |        format: int32
    |        required: true
    |        default: 2
    |      responses:
    |        '200':
    |          description: successful operation
    |          schema:
    |            "$$ref": "#/definitions/Order"
    |        '400':
    |          description: Invalid ID supplied
    |        '404':
    |          description: Order not found
    |    delete:
    |      tags:
    |      - store
    |      x-jvm-package: store
    |      summary: Delete purchase order by ID
    |      description: For valid response try integer IDs with positive integer value.
    |        Negative or non-integer values will generate API errors
    |      operationId: deleteOrder
    |      produces:
    |      - application/xml
    |      - application/json
    |      parameters:
    |      - name: order_id
    |        in: path
    |        description: ID of the order that needs to be deleted
    |        required: true
    |        type: integer
    |        minimum: 1
    |        format: int64
    |      responses:
    |        '400':
    |          description: Invalid ID supplied
    |        '404':
    |          description: Order not found
    |securityDefinitions:
    |  petstore_auth:
    |    type: oauth2
    |    authorizationUrl: http://petstore.swagger.io/oauth/dialog
    |    flow: implicit
    |    scopes:
    |      write:pets: modify pets in your account
    |      read:pets: read your pets
    |  api_key:
    |    type: apiKey
    |    name: api_key
    |    in: header
    |definitions:
    |  Order:
    |    type: object
    |    properties:
    |      id:
    |        type: integer
    |        format: int64
    |      petId:
    |        type: integer
    |        format: int64
    |      quantity:
    |        type: integer
    |        format: int32
    |      shipDate:
    |        type: string
    |        format: date-time
    |      status:
    |        type: string
    |        description: Order Status
    |        enum:
    |        - placed
    |        - approved
    |        - delivered
    |      complete:
    |        type: boolean
    |        default: false
    |    xml:
    |      name: Order
    |""".stripMargin

  test("Ensure responses are generated") {
    val (
      _,
      Clients(Client(tags, className, _, staticDefns, cls, _) :: _, Nil),
      _
    ) = runSwaggerSpec(scalaInterpreter)(swagger)(Context.empty, "akka-http")
    val cmp = companionForStaticDefns(staticDefns)

    tags should equal(Seq("store"))

    val companion = q"""
      object StoreClient {
        def apply(host: String = "http://petstore.swagger.io")(implicit httpClient: HttpRequest => Future[HttpResponse], ec: ExecutionContext, mat: Materializer): StoreClient =
          new StoreClient(host = host)(httpClient = httpClient, ec = ec, mat = mat)
        def httpClient(httpClient: HttpRequest => Future[HttpResponse], host: String = "http://petstore.swagger.io")(implicit ec: ExecutionContext, mat: Materializer): StoreClient =
          new StoreClient(host = host)(httpClient = httpClient, ec = ec, mat = mat)
      }
    """

    val client = q"""
      class StoreClient(host: String = "http://petstore.swagger.io")(implicit httpClient: HttpRequest => Future[HttpResponse], ec: ExecutionContext, mat: Materializer) {
        val basePath: String = ""
        private[this] def makeRequest[T: ToEntityMarshaller](method: HttpMethod, uri: Uri, headers: scala.collection.immutable.Seq[HttpHeader], entity: T, protocol: HttpProtocol): EitherT[Future, Either[Throwable, HttpResponse], HttpRequest] = {
          EitherT(Marshal(entity).to[RequestEntity].map[Either[Either[Throwable, HttpResponse], HttpRequest]] {
            entity => Right(HttpRequest(method = method, uri = uri, headers = headers, entity = entity, protocol = protocol))
          }.recover({
            case t =>
              Left(Left(t))
          }))
        }
        val getOrderByIdOKDecoder = {
          structuredJsonEntityUnmarshaller.flatMap(_ => _ => json => io.circe.Decoder[Order].decodeJson(json).fold(FastFuture.failed, FastFuture.successful))
        }
        def getOrderById(orderId: Long, defparmOpt: Option[Int] = Option(1), defparm: Int = 2, headerMeThis: String, headers: List[HttpHeader] = Nil): EitherT[Future, Either[Throwable, HttpResponse], GetOrderByIdResponse] = {
          val allHeaders = headers ++ scala.collection.immutable.Seq[Option[HttpHeader]](Some(RawHeader("HeaderMeThis", Formatter.show(headerMeThis)))).flatten
          makeRequest(HttpMethods.GET, host + basePath + "/store/order/" + Formatter.addPath(orderId) + "?" + Formatter.addArg("defparm_opt", defparmOpt) + Formatter.addArg("defparm", defparm), allHeaders, HttpEntity.Empty, HttpProtocols.`HTTP/1.1`).flatMap(req => EitherT(httpClient(req).flatMap(resp => resp.status match {
            case StatusCodes.OK =>
              Unmarshal(resp.entity).to[Order](getOrderByIdOKDecoder, implicitly, implicitly).map(x => Right(GetOrderByIdResponse.OK(x)))
            case StatusCodes.BadRequest =>
              resp.discardEntityBytes().future.map(_ => Right(GetOrderByIdResponse.BadRequest))
            case StatusCodes.NotFound =>
              resp.discardEntityBytes().future.map(_ => Right(GetOrderByIdResponse.NotFound))
            case _ =>
              FastFuture.successful(Left(Right(resp)))
          }).recover({
            case e: Throwable =>
              Left(Left(e))
          })))
        }
        def deleteOrder(orderId: Long, headers: List[HttpHeader] = Nil): EitherT[Future, Either[Throwable, HttpResponse], DeleteOrderResponse] = {
          val allHeaders = headers ++ scala.collection.immutable.Seq[Option[HttpHeader]]().flatten
          makeRequest(HttpMethods.DELETE, host + basePath + "/store/order/" + Formatter.addPath(orderId), allHeaders, HttpEntity.Empty, HttpProtocols.`HTTP/1.1`).flatMap(req => EitherT(httpClient(req).flatMap(resp => resp.status match {
            case StatusCodes.BadRequest =>
              resp.discardEntityBytes().future.map(_ => Right(DeleteOrderResponse.BadRequest))
            case StatusCodes.NotFound =>
              resp.discardEntityBytes().future.map(_ => Right(DeleteOrderResponse.NotFound))
            case _ =>
              FastFuture.successful(Left(Right(resp)))
          }).recover({
            case e: Throwable =>
              Left(Left(e))
          })))
        }
      }
    """

    cmp.structure should equal(companion.structure)
    cls.head.value.structure should equal(client.structure)
  }
}
