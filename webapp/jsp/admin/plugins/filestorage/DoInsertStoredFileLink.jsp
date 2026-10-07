<%@ page errorPage="../../ErrorPage.jsp" %>

${ pageContext.response.sendRedirect( fileStorageInsertServiceJspBean.doInsertLink( pageContext.request ) ) }
