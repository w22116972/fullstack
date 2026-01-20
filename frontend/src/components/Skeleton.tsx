import { FC } from 'react';

interface SkeletonProps {
    width?: string | number;
    height?: string | number;
    borderRadius?: string;
    className?: string;
}

const Skeleton: FC<SkeletonProps> = ({
    width = '100%',
    height = '1em',
    borderRadius = '4px',
    className = '',
}) => {
    const style: React.CSSProperties = {
        width,
        height,
        borderRadius,
        backgroundColor: '#e0e0e0',
        animation: 'pulse 1.5s ease-in-out infinite',
    };

    return (
        <>
            <style>
                {`
                    @keyframes pulse {
                        0%, 100% { opacity: 1; }
                        50% { opacity: 0.5; }
                    }
                `}
            </style>
            <div style={style} className={className} aria-hidden="true" />
        </>
    );
};

// Table row skeleton for article list
export const ArticleRowSkeleton: React.FC = () => (
    <tr style={{ borderBottom: '1px solid #eee' }}>
        <td style={{ padding: '10px' }}><Skeleton width="80%" /></td>
        <td style={{ padding: '10px' }}><Skeleton width="60%" /></td>
        <td style={{ padding: '10px' }}><Skeleton width="50px" /></td>
        <td style={{ padding: '10px' }}><Skeleton width="80px" /></td>
        <td style={{ padding: '10px' }}>
            <div style={{ display: 'flex', gap: '10px' }}>
                <Skeleton width="40px" />
                <Skeleton width="50px" />
            </div>
        </td>
    </tr>
);

// Table skeleton for article list
export const ArticleTableSkeleton: React.FC<{ rows?: number }> = ({ rows = 5 }) => (
    <table style={{ width: '100%', borderCollapse: 'collapse' }} aria-label="Loading articles">
        <thead>
            <tr style={{ borderBottom: '1px solid #ccc' }}>
                <th style={{ textAlign: 'left', padding: '10px' }}>Title</th>
                <th style={{ textAlign: 'left', padding: '10px' }}>Author</th>
                <th style={{ textAlign: 'left', padding: '10px' }}>Status</th>
                <th style={{ textAlign: 'left', padding: '10px' }}>Created At</th>
                <th style={{ textAlign: 'left', padding: '10px' }}>Actions</th>
            </tr>
        </thead>
        <tbody>
            {Array.from({ length: rows }).map((_, index) => (
                <ArticleRowSkeleton key={index} />
            ))}
        </tbody>
    </table>
);

// Form skeleton
export const FormSkeleton: React.FC = () => (
    <div style={{ maxWidth: '600px', margin: 'auto', padding: '20px' }}>
        <Skeleton width="150px" height="2em" />
        <div style={{ marginTop: '20px' }}>
            <Skeleton width="100px" height="1em" />
            <Skeleton height="40px" />
        </div>
        <div style={{ marginTop: '20px' }}>
            <Skeleton width="100px" height="1em" />
            <Skeleton height="200px" />
        </div>
        <div style={{ marginTop: '20px' }}>
            <Skeleton width="100px" height="40px" />
        </div>
    </div>
);

export default Skeleton;
