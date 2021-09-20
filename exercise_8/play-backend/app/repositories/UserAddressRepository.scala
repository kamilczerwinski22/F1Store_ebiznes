package repositories

import models.UserAddress
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import java.sql.Timestamp
import java.time.Instant
import java.util.Date
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserAddressRepository @Inject()(dbConfigProvider: DatabaseConfigProvider, val userRepository: UserRepository)(implicit ec: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  class UserAddressTable(tag: Tag) extends Table[UserAddress](tag, "user_address") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def userId = column[Long]("user_id")
    def userId_fk = foreignKey("user_fk", userId, user)(_.id)
    def firstname = column[String]("firstname")
    def lastname = column[String]("lastname")
    def address = column[String]("address")
    def zipcode = column[String]("zipcode")
    def city = column[String]("city")
    def country = column[String]("country")

    def * = (id, userId, firstname, lastname, address, zipcode, city, country) <> ((UserAddress.apply _).tupled, UserAddress.unapply)
  }

  import userRepository.UserTable

  val userAddress = TableQuery[UserAddressTable]
  val user = TableQuery[UserTable]

  def list(): Future[Seq[UserAddress]] = db.run {
    userAddress.result
  }

  def listByUserId(userId: Long): Future[Seq[UserAddress]] = db.run {
    userAddress.filter(_.userId === userId).result
  }

  def create(userId: Long, firstname: String, lastname: String, address: String, zipcode: String, city: String, country: String): Future[UserAddress] = db.run {
    (userAddress.map(a => (a.userId, a.firstname, a.lastname, a.address, a.zipcode, a.city, a.country))
      returning userAddress.map(_.id)
      into { case ((userId, firstname, lastname, address, zipcode, city, country), id) => UserAddress(id, userId, firstname, lastname, address, zipcode, city, country) }
      ) += (userId, firstname, lastname, address, zipcode, city, country)
  }

  def getByIdOption(id: Long): Future[Option[UserAddress]] = db.run {
    userAddress.filter(_.id === id).result.headOption
  }

  def update(id: Long, new_userAddress: UserAddress): Future[Int] = {
    val userAddressToUpdate: UserAddress = new_userAddress.copy(id)
    db.run(userAddress.filter(_.id === id).update(userAddressToUpdate))
  }

  def delete(id: Long): Future[Int] = db.run(userAddress.filter(_.id === id).delete)
}
