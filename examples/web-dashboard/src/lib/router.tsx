import {
  useSyncExternalStore,
  type AnchorHTMLAttributes,
  type MouseEvent,
  type ReactNode,
} from "react";

const NAVIGATION_EVENT = "app:navigate";

const subscribe = (listener: () => void) => {
  window.addEventListener("popstate", listener);
  window.addEventListener(NAVIGATION_EVENT, listener);

  return () => {
    window.removeEventListener("popstate", listener);
    window.removeEventListener(NAVIGATION_EVENT, listener);
  };
};

const getPathname = () => window.location.pathname;
const getServerPathname = () => "/";

export const useLocation = () => ({
  pathname: useSyncExternalStore(subscribe, getPathname, getServerPathname),
});

const navigate = (to: string) => {
  window.history.pushState(null, "", to);
  window.dispatchEvent(new Event(NAVIGATION_EVENT));
  window.scrollTo({ top: 0 });
};

type LinkProps = Omit<AnchorHTMLAttributes<HTMLAnchorElement>, "href"> & {
  to: string;
};

export const Link = ({ to, onClick, target, ...props }: LinkProps) => {
  const handleClick = (event: MouseEvent<HTMLAnchorElement>) => {
    onClick?.(event);
    if (
      event.defaultPrevented ||
      event.button !== 0 ||
      event.metaKey ||
      event.ctrlKey ||
      event.shiftKey ||
      event.altKey ||
      target === "_blank"
    ) {
      return;
    }

    event.preventDefault();
    navigate(to);
  };

  return <a href={to} target={target} onClick={handleClick} {...props} />;
};

type NavLinkState = { isActive: boolean };

type NavLinkProps = Omit<LinkProps, "children" | "className"> & {
  end?: boolean;
  className?: string | ((state: NavLinkState) => string);
  children?: ReactNode | ((state: NavLinkState) => ReactNode);
};

export const NavLink = ({ to, end = false, className, children, ...props }: NavLinkProps) => {
  const { pathname } = useLocation();
  const isActive = end ? pathname === to : pathname === to || pathname.startsWith(`${to}/`);
  const state = { isActive };

  return (
    <Link
      to={to}
      aria-current={isActive ? "page" : undefined}
      className={typeof className === "function" ? className(state) : className}
      {...props}
    >
      {typeof children === "function" ? children(state) : children}
    </Link>
  );
};
